package com.socialweb.security;

/** 当前登录用户（ThreadLocal），未登录时为 null */
public final class CurrentUser {

    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    private CurrentUser() {
    }

    public static void set(Long userId) {
        HOLDER.set(userId);
    }

    public static Long get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
