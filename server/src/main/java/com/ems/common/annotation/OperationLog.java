package com.ems.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解。
 * 标注在 Controller 方法上,由 {@link OperationLogAspect} 切面拦截并记录操作日志。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 模块名(如:用户管理、角色管理)
     */
    String module();

    /**
     * 操作描述(如:新增、修改、删除)
     */
    String operation();
}
