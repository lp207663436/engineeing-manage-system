package com.ems.common.datascope;

/**
 * 数据权限上下文(方法级 ThreadLocal)。
 * DataScopeAspect 在 Controller 方法执行前设置,Service 层通过 DataScopeHelper 读取。
 */
public class DataScopeContext {

    private static final ThreadLocal<String> CREATE_BY_FIELD = new ThreadLocal<>();

    public static void set(String createByField) {
        CREATE_BY_FIELD.set(createByField);
    }

    public static String get() {
        return CREATE_BY_FIELD.get();
    }

    public static void clear() {
        CREATE_BY_FIELD.remove();
    }
}
