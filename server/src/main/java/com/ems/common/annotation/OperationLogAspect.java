package com.ems.common.annotation;

import com.ems.module.system.entity.SysOperationLog;
import com.ems.module.system.service.SysOperationLogService;
import com.ems.security.context.SecurityContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志 AOP 切面。
 * 拦截标注了 {@link OperationLog} 的方法,记录操作日志(用户、模块、操作、参数、结果、IP、耗时)。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final SysOperationLogService operationLogService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        SysOperationLog logEntity = new SysOperationLog();
        logEntity.setModule(operationLog.module());
        logEntity.setOperation(operationLog.operation());
        logEntity.setCreateTime(LocalDateTime.now());

        // 操作人信息
        if (SecurityContext.get() != null) {
            logEntity.setUserId(SecurityContext.getUserId());
            logEntity.setUsername(SecurityContext.get().getUsername());
        }

        // 请求方法
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        logEntity.setMethod(method.getDeclaringClass().getSimpleName() + "." + method.getName());

        // 请求参数(序列化为 JSON,限制长度避免过大)
        try {
            String params = objectMapper.writeValueAsString(joinPoint.getArgs());
            if (params.length() > 2000) {
                params = params.substring(0, 2000) + "...";
            }
            logEntity.setParams(params);
        } catch (Exception e) {
            logEntity.setParams("参数序列化失败");
        }

        // 请求 IP
        logEntity.setIp(resolveClientIp());

        Object result;
        try {
            result = joinPoint.proceed();
            logEntity.setResult("成功");
        } catch (Throwable e) {
            logEntity.setResult("失败:" + e.getMessage());
            throw e;
        } finally {
            logEntity.setCostTime(System.currentTimeMillis() - startTime);
            // 异步写入日志
            try {
                operationLogService.insert(logEntity);
            } catch (Exception e) {
                log.warn("操作日志写入异常: {}", e.getMessage());
            }
        }
        return result;
    }

    /**
     * 解析客户端真实 IP(兼容反向代理)
     */
    private String resolveClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "unknown";
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        } catch (Exception e) {
            return "unknown";
        }
    }
}
