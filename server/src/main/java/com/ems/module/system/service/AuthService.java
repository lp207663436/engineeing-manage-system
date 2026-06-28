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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * JWT 过期时间(秒),与 Redis 中 token 缓存 TTL 保持一致。
     */
    @Value("${jwt.expire-seconds:86400}")
    private long jwtExpireSeconds;

    public LoginVO login(LoginDTO dto) {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (user == null) throw new BusinessException(401, "用户名或密码错误");
        if (user.getStatus() != null && user.getStatus() == 0) throw new BusinessException(401, "账号已禁用");
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        String token = jwtUtil.generate(user.getId(), user.getUsername());
        redisTemplate.opsForValue().set("token:" + user.getId(), token, Duration.ofSeconds(jwtExpireSeconds));
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setName(user.getName());
        return vo;
    }

    public void logout(Long userId) {
        redisTemplate.delete("token:" + userId);
    }
}
