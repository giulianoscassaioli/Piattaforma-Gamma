package com.gamma.firma.tenant;

public final class TenantContext {

    private static final ThreadLocal<TenantInfo> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(TenantInfo info) {
        CURRENT.set(info);
    }

    public static TenantInfo get() {
        return CURRENT.get();
    }

    public static String getTenantId() {
        TenantInfo info = CURRENT.get();
        return info != null ? info.getTenantId() : null;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
