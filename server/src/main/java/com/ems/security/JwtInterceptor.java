package com.ems.security;

import com.ems.common.constant.SecurityConstant;
import com.ems.common.exception.BusinessException;
import com.ems.module.system.entity.SysRole;
import com.ems.module.system.entity.SysUser;
import com.ems.module.system.entity.SysUserRole;
import com.ems.module.system.mapper.SysRoleMapper;
import com.ems.module.system.mapper.SysUserMapper;
import com.ems.module.system.mapper.SysUserRoleMapper;
import com.ems.module.system.service.SysMenuService;
import com.ems.security.context.CurrentUser;
import com.ems.security.context.SecurityContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysMenuService sysMenuService;

    /** 用户信息缓存 TTL,与 token TTL 对齐(30 分钟) */
    private static final Duration USER_INFO_TTL = Duration.ofMinutes(30);
    private static final String USER_INFO_KEY_PREFIX = "user:info:";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader(SecurityConstant.HEADER_AUTH);
        if (!StringUtils.hasText(header) || !header.startsWith(SecurityConstant.TOKEN_PREFIX)) {
            throw new BusinessException(401, "未登录");
        }
        String token = header.substring(SecurityConstant.TOKEN_PREFIX.length());
        Long userId = jwtUtil.parseUserId(token);
        if (userId == null) throw new BusinessException(401, "token 无效");
        Object cached = redisTemplate.opsForValue().get("token:" + userId);
        if (cached == null || !token.equals(cached)) {
            throw new BusinessException(401, "token 已失效,请重新登录");
        }

        // 优先从 Redis 缓存读取用户基本信息(deptId, dataScope, permissions),miss 再查库并回填
        String userInfoKey = USER_INFO_KEY_PREFIX + userId;
        CurrentUser cachedUser = null;
        try {
            Object obj = redisTemplate.opsForValue().get(userInfoKey);
            if (obj instanceof CurrentUser) {
                cachedUser = (CurrentUser) obj;
            }
        } catch (Exception e) {
            log.warn("读取用户信息缓存失败,将回退查库: {}", e.getMessage());
        }

        CurrentUser user;
        if (cachedUser != null) {
            // 缓存命中,直接使用
            user = cachedUser;
        } else {
            // 缓存未命中,查询数据库并回填
            SysUser sysUser = sysUserMapper.selectById(userId);
            if (sysUser == null) {
                redisTemplate.delete("token:" + userId);
                throw new BusinessException(401, "用户不存在");
            }
            if (sysUser.getStatus() != null && sysUser.getStatus() == 0) {
                redisTemplate.delete("token:" + userId);
                throw new BusinessException(401, "账号已被禁用");
            }
            Long deptId = sysUser.getDeptId();
            Integer dataScope = resolveDataScope(userId);
            // 填充 permissions 字段(修复23:CurrentUser.permissions 未被填充)
            Set<String> permissions = sysMenuService.getPermissionsByUserId(userId);

            user = CurrentUser.builder()
                    .userId(userId)
                    .username(jwtUtil.parseUsername(token))
                    .deptId(deptId)
                    .dataScope(dataScope)
                    .permissions(permissions)
                    .build();
            // 回填缓存
            try {
                redisTemplate.opsForValue().set(userInfoKey, user, USER_INFO_TTL);
            } catch (Exception e) {
                log.warn("回填用户信息缓存失败: {}", e.getMessage());
            }
        }
        SecurityContext.set(user);
        return true;
    }

    /**
     * 取当前用户所有角色的 dataScope,多角色取最宽(数值最小,1=全部最宽)。
     * 超管(SUPER_ADMIN_ID)直接返回 1。
     */
    private Integer resolveDataScope(Long userId) {
        if (SecurityContext.SUPER_ADMIN_ID.equals(userId)) return 1;
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (userRoles.isEmpty()) return 4; // 无角色默认仅本人
        Set<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
        List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
        return roles.stream()
                .map(SysRole::getDataScope)
                .filter(s -> s != null)
                .min(Comparator.naturalOrder())
                .orElse(4);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        SecurityContext.clear();
    }
}
