package com.pharmapos.sharedkernel;

import java.util.Objects;

/**
 * ThreadLocal-based holder for the current request's {@link TenantContext}.
 *
 * <p>The tenant context is set at the start of each request by a servlet filter
 * or interceptor and cleared at the end (typically in a {@code finally} block
 * or a filter's post-processing).
 *
 * <p>Background jobs and outbox consumers rebuild the context from the event's
 * tenant information before calling {@link #set(TenantContext)}.
 *
 * <p>Thread safety: each thread sees its own context via the underlying
 * {@link ThreadLocal}. For virtual threads or reactive contexts, a custom
 * propagation mechanism would be needed.
 */
public final class TenantContextHolder {

    private static final ThreadLocal<TenantContext> HOLDER = new ThreadLocal<>();

    private TenantContextHolder() {
        // utility class — not instantiable
    }

    /**
     * Sets the tenant context for the current thread.
     *
     * @param context the tenant context (must not be null)
     * @throws NullPointerException if context is null
     */
    public static void set(TenantContext context) {
        Objects.requireNonNull(context, "TenantContext must not be null");
        HOLDER.set(context);
    }

    /**
     * Returns the tenant context for the current thread.
     *
     * @return the current tenant context
     * @throws IllegalStateException if no context has been set
     */
    public static TenantContext require() {
        TenantContext ctx = HOLDER.get();
        if (ctx == null) {
            throw new IllegalStateException(
                    "No TenantContext set for the current thread. "
                            + "Ensure the tenant filter has run before accessing the context.");
        }
        return ctx;
    }

    /**
     * Returns the tenant context for the current thread, or null if none is set.
     *
     * @return the current tenant context, or null
     */
    public static TenantContext get() {
        return HOLDER.get();
    }

    /**
     * Clears the tenant context for the current thread.
     * Must be called in a {@code finally} block at the end of each request.
     */
    public static void clear() {
        HOLDER.remove();
    }
}
