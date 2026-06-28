package com.ems.security;

import com.ems.common.constant.SecurityConstant;
import com.ems.common.exception.BusinessException;
import com.ems.module.system.entity.SysRole;
import com.ems.module.system.entity.SysUser;
import com.ems.module.system.entity.SysUserRole;
import com.ems.module.system.mapper.SysRoleMapper;
import com.ems.module.system.mapper.SysUserMapper;
import com.ems.module.system.mapper.SysUserRoleMapper;
import com.ems.security.context.CurrentUser;
import com.ems.security.context.SecurityContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;

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

        // 查询用户部门 + 角色数据范围,填充到 SecurityContext
        SysUser sysUser = sysUserMapper.selectById(userId);
        Long deptId = sysUser == null ? null : sysUser.getDeptId();
        Integer dataScope = resolveDataScope(userId);

        CurrentUser user = CurrentUser.builder()
                .userId(userId)
                .username(jwtUtil.parseUsername(token))
                .deptId(deptId)
                .dataScope(dataScope)
                .build();
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
        if (userRoles.isEmpty()) return 3; // 无角色默认仅本人
        Set<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
        List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
        return roles.stream()
                .map(SysRole::getDataScope)
                .filter(s -> s != null)
                .min(Comparator.naturalOrder())
                .orElse(3);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        SecurityContext.clear();
    }
}
