package com.pharmapos.shared.context;

/**
 * Thread-local holder for TenantContext.
 * Populated by security filter after token validation.
 * Cleared at the end of each request.
 */
public final class TenantContextHolder {

    private static final ThreadLocal<TenantContext> contextHolder = new ThreadLocal<>();

    private TenantContextHolder() {
        // Utility class
    }

    /**
     * Set the current tenant context.
     * Called by security filter.
     */
    public static void setContext(TenantContext context) {
        if (context == null) {
            throw new IllegalArgumentException("TenantContext cannot be null");
        }
        contextHolder.set(context);
    }

    /**
     * Get the current tenant context.
     * @throws IllegalStateException if no context is set
     */
    public static TenantContext getContext() {
        TenantContext context = contextHolder.get();
        if (context == null) {
            throw new IllegalStateException("No TenantContext available - authentication required");
        }
        return context;
    }

    /**
     * Get the current tenant context, or null if not set.
     */
    public static TenantContext getContextOrNull() {
        return contextHolder.get();
    }

    /**
     * Clear the current tenant context.
     * Must be called at the end of each request.
     */
    public static void clear() {
        contextHolder.remove();
    }

    /**
     * Check if a context is currently set.
     */
    public static boolean hasContext() {
        return contextHolder.get() != null;
    }
}
