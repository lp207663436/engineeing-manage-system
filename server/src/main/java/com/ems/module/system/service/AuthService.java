package com.ems.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ems.common.exception.BusinessException;
import com.ems.module.system.dto.LoginDTO;
import com.ems.module.system.dto.LoginVO;
import com.ems.module.system.entity.SysUser;
import com.ems.module.system.mapper.SysUserMapper;
import com.ems.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    /** 注入单例 BCryptPasswordEncoder,避免每次 new 一个实例(由 SecurityConfig 提供) */
    private final BCryptPasswordEncoder passwordEncoder;

    /** 登录失败次数上限,超过则锁定账号 */
    private static final int LOGIN_FAIL_LIMIT = 5;
    /** 账号锁定时长(分钟) */
    private static final long LOCK_MINUTES = 30;

    /**
     * JWT 过期时间(秒),与 Redis 中 token 缓存 TTL 保持一致。
     */
    @Value("${ems.jwt.expire:86400}")
    private long jwtExpireSeconds;

    public LoginVO login(LoginDTO dto) {
        // 防暴力破解:先检查账号是否已被锁定
        String failKey = "login:fail:" + dto.getUsername();
        String failCountStr = stringRedisTemplate.opsForValue().get(failKey);
        int failCount = failCountStr == null ? 0 : Integer.parseInt(failCountStr);
        if (failCount >= LOGIN_FAIL_LIMIT) {
            Long ttl = stringRedisTemplate.getExpire(failKey);
            long remainMinutes = ttl == null || ttl < 0 ? LOCK_MINUTES : (ttl + 59) / 60;
            throw new BusinessException("账号已锁定,请" + remainMinutes + "分钟后再试");
        }

        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (user == null) {
            recordLoginFail(failKey, failCount);
            throw new BusinessException(401, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(401, "账号已禁用");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            recordLoginFail(failKey, failCount);
            throw new BusinessException(401, "用户名或密码错误");
        }
        // 登录成功:清除失败计数
        stringRedisTemplate.delete(failKey);

        String token = jwtUtil.generate(user.getId(), user.getUsername());
        redisTemplate.opsForValue().set("token:" + user.getId(), token, Duration.ofSeconds(jwtExpireSeconds));
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setName(user.getName());
        return vo;
    }

    /**
     * 记录登录失败次数,达到上限则锁定 30 分钟。
     */
    private void recordLoginFail(String failKey, int currentFailCount) {
        int newCount = currentFailCount + 1;
        if (newCount >= LOGIN_FAIL_LIMIT) {
            // 达到上限,设置锁定 TTL
            stringRedisTemplate.opsForValue().set(failKey, String.valueOf(newCount), Duration.ofMinutes(LOCK_MINUTES));
        } else {
            // 未达上限,首次失败时设置 30 分钟 TTL(避免无限累计),后续累加时刷新 TTL 不合理,改为仅在首次设置
            if (currentFailCount == 0) {
                stringRedisTemplate.opsForValue().set(failKey, String.valueOf(newCount), Duration.ofMinutes(LOCK_MINUTES));
            } else {
                stringRedisTemplate.opsForValue().set(failKey, String.valueOf(newCount));
            }
        }
    }

    public void logout(Long userId) {
        redisTemplate.delete("token:" + userId);
        // 同步清除用户信息缓存
        redisTemplate.delete("user:info:" + userId);
    }
}
