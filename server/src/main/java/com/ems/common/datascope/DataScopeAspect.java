package com.ems.common.datascope;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 数据权限切面。
 * 拦截标注 @DataScope 的 Controller 方法,在执行前把 createByField 放入 ThreadLocal,
 * Service 层调用 DataScopeHelper.applyTo(wrapper) 即可读取并应用过滤。
 */
@Slf4j
@Aspect
@Component
public class DataScopeAspect {

    @Around("@annotation(dataScope)")
    public Object around(ProceedingJoinPoint joinPoint, DataScope dataScope) throws Throwable {
        DataScopeContext.set(dataScope.createByField());
        try {
            return joinPoint.proceed();
        } finally {
            DataScopeContext.clear();
        }
    }
}
