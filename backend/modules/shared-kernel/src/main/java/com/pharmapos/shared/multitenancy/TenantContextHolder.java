package com.pharmapos.shared.multitenancy;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Objects;

/**
 * ThreadLocal carrier for the current request's {@link TenantContext}.
 *
 * <p>The static API (get / set / clear) is used by infrastructure code that runs on
 * non-Spring threads (async task executors, outbox relays). The Spring
 * {@code @RequestScope} bean delegates to the same ThreadLocal so services can
 * inject it via constructor injection for testing convenience.
 *
 * <p><b>Contract:</b> {@link TenantContextFilter} sets the context before the first
 * handler call and clears it in a {@code finally} block. Code that spawns background
 * threads must propagate the context explicitly before the thread executes.
 */
@Component
@RequestScope
public class TenantContextHolder {

    private static final ThreadLocal<TenantContext> HOLDER = new ThreadLocal<>();

    // ── Static API ──────────────────────────────────────────────────────────

    /**
     * Returns the current context.
     *
     * @throws IllegalStateException if no context is set on this thread
     */
    public static TenantContext get() {
        TenantContext ctx = HOLDER.get();
        if (ctx == null) {
            throw new IllegalStateException(
                "No TenantContext on this thread. " +
                "Ensure TenantContextFilter has run, or propagate the context explicitly " +
                "when spawning background threads.");
        }
        return ctx;
    }

    /**
     * Returns the current context or {@code null} — safe to call outside request scope
     * (e.g. health checks, platform endpoints).
     */
    public static TenantContext peek() {
        return HOLDER.get();
    }

    /** Sets the context for the current thread. Must not be null. */
    public static void set(TenantContext ctx) {
        Objects.requireNonNull(ctx, "TenantContext must not be null; call clear() to remove it");
        HOLDER.set(ctx);
    }

    /**
     * Removes the context from the current thread. <b>Always call this in a
     * {@code finally} block</b> to prevent context leaking across pooled threads.
     */
    public static void clear() {
        HOLDER.remove();
    }

    // ── Instance API (for injection) ─────────────────────────────────────────

    /** @see #get() */
    public TenantContext current() {
        return get();
    }

    /** @see #peek() */
    public TenantContext currentOrNull() {
        return peek();
    }
}
