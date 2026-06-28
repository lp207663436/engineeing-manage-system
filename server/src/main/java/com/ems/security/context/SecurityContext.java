package com.ems.security.context;

public class SecurityContext {

    /**
     * 超级管理员用户 ID,集中管理避免硬编码散落各处。
     */
    public static final Long SUPER_ADMIN_ID = 1L;

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    public static void set(CurrentUser user) { HOLDER.set(user); }
    public static CurrentUser get() { return HOLDER.get(); }
    public static Long getUserId() {
        CurrentUser u = get();
        return u == null ? null : u.getUserId();
    }

    /**
     * 判断当前线程用户是否为超级管理员。
     */
    public static boolean isAdmin() {
        CurrentUser u = get();
        return u != null && SUPER_ADMIN_ID.equals(u.getUserId());
    }

    public static void clear() { HOLDER.remove(); }
}
