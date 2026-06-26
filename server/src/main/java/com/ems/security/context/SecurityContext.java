package com.ems.security.context;

public class SecurityContext {
    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    public static void set(CurrentUser user) { HOLDER.set(user); }
    public static CurrentUser get() { return HOLDER.get(); }
    public static Long getUserId() {
        CurrentUser u = get();
        return u == null ? null : u.getUserId();
    }
    public static void clear() { HOLDER.remove(); }
}
