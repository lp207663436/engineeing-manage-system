package com.ems.security;

import com.ems.common.exception.BusinessException;
import com.ems.security.annotation.RequirePermission;
import com.ems.security.context.SecurityContext;
import com.ems.module.system.service.SysMenuService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private final SysMenuService menuService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) return true;
        RequirePermission annotation = method.getMethodAnnotation(RequirePermission.class);
        if (annotation == null) annotation = method.getBeanType().getAnnotation(RequirePermission.class);
        if (annotation == null) return true;

        Long userId = SecurityContext.getUserId();
        if (userId == null) throw new BusinessException(401, "未登录");
        if (SecurityContext.isAdmin()) return true; // 超管放行

        Set<String> permissions = menuService.getPermissionsByUserId(userId);
        if (!permissions.contains(annotation.value()) && !permissions.contains("*")) {
            throw new BusinessException(403, "无权限: " + annotation.value());
        }
        return true;
    }
}
