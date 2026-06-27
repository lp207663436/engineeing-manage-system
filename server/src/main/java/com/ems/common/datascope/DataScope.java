package com.ems.common.datascope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限注解。
 * 标注在 Controller 的分页查询方法上,触发 DataScopeAspect 在 ThreadLocal 中
 * 注入当前用户的数据范围信息,Service 层调用 DataScopeHelper.applyTo 即可应用过滤。
 *
 * dataScope 取值(与 sys_role.data_scope 注释一致):
 * 1 = 全部数据(超管/全局)
 * 2 = 本部门
 * 3 = 本部门及以下
 * 4 = 本人
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {
    /**
     * 业务表中创建人字段名,默认 create_by
     */
    String createByField() default "create_by";
}
