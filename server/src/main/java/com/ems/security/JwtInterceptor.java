package com.ems.security;

import com.ems.common.constant.SecurityConstant;
import com.ems.common.exception.BusinessException;
import com.ems.security.context.CurrentUser;
import com.ems.security.context.SecurityContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

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
        CurrentUser user = CurrentUser.builder()
                .userId(userId)
                .username(jwtUtil.parseUsername(token))
                .build();
        SecurityContext.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        SecurityContext.clear();
    }
}
