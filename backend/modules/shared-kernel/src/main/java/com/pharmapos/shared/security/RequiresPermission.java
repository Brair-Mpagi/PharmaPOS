package com.pharmapos.shared.security;

import java.lang.annotation.*;

/**
 * Marks a method or type that requires the caller to hold one or more
 * named permissions. The {@link PermissionEnforcementAspect} intercepts
 * annotated methods, reads the current {@link com.pharmapos.shared.multitenancy.TenantContext},
 * and throws {@link org.springframework.security.access.AccessDeniedException}
 * if the caller is missing any required permission.
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * @RequiresPermission("sales.create")
 * public SaleResponse finaliseSale(FinaliseSaleCommand cmd) { ... }
 *
 * // Multiple permissions — ALL must be held:
 * @RequiresPermission({"proc.order", "proc.approve"})
 * public void approveOrder(UUID orderId) { ... }
 *
 * // With a value limit (e.g. refund cap):
 * @RequiresPermission(value = "sales.refund", limit = "refund.cap")
 * public RefundResponse issueRefund(RefundCommand cmd) { ... }
 * }</pre>
 *
 * <p>The annotation may be placed on the class to apply to all its methods
 * (overridden by a method-level annotation when both are present).
 *
 * <p><b>Build rule (TDD §3.3):</b> every controller method must carry this
 * annotation or be on a class that carries it — the CI build rejects any
 * controller endpoint that lacks one.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {

    /**
     * One or more permission strings that the caller must hold.
     * All listed permissions are required (logical AND).
     * Permission strings match those in {@link com.pharmapos.shared.multitenancy.TenantContext#roles()}.
     */
    String[] value();

    /**
     * Optional named value-limit key (e.g. {@code "refund.cap"} or {@code "discount.max"}).
     * When set, the aspect also checks that the relevant limit from the caller's role
     * is not violated. The concrete limit check is delegated to
     * {@link com.pharmapos.shared.security.PermissionEnforcementAspect}.
     */
    String limit() default "";
}
