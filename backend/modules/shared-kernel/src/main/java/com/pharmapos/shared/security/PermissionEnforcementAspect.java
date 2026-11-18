package com.pharmapos.shared.security;

import com.pharmapos.shared.multitenancy.TenantContext;
import com.pharmapos.shared.multitenancy.TenantContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * AOP aspect that enforces {@link RequiresPermission} annotations on service
 * and controller methods (TDD §9.2).
 *
 * <p>The aspect reads the current {@link TenantContext} from the
 * {@link TenantContextHolder} and verifies that the caller holds every
 * permission listed in the annotation. If any permission is missing it
 * throws {@link AccessDeniedException} immediately, before any SQL is issued.
 *
 * <p><b>Ordering:</b> runs at {@code HIGHEST_PRECEDENCE + 15} — intentionally
 * <em>before</em> {@link com.pharmapos.shared.multitenancy.TenantTransactionInterceptor}
 * (+20) so that unauthorised calls are rejected before a transaction is opened
 * and before RLS session variables are set. This guarantees fail-fast behaviour
 * on authorisation failures.
 *
 * <h3>Method-vs-class annotation precedence:</h3>
 * A method-level {@code @RequiresPermission} overrides a class-level one when
 * both are present. This allows a controller to declare a base permission at
 * the class level and refine it per method.
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 15)
public class PermissionEnforcementAspect implements Ordered {

    private static final Logger log = LoggerFactory.getLogger(PermissionEnforcementAspect.class);

    @Around("@annotation(com.pharmapos.shared.security.RequiresPermission) " +
            "|| @within(com.pharmapos.shared.security.RequiresPermission)")
    public Object enforce(ProceedingJoinPoint pjp) throws Throwable {
        RequiresPermission annotation = resolveAnnotation(pjp);
        if (annotation == null) {
            return pjp.proceed();
        }

        TenantContext ctx = TenantContextHolder.peek();
        if (ctx == null) {
            // No tenant context — platform thread or test harness. Fail safe.
            throw new AccessDeniedException(
                "No TenantContext: cannot enforce permission at " +
                pjp.getSignature().toShortString());
        }

        String[] requiredPermissions = annotation.value();
        for (String permission : requiredPermissions) {
            if (!ctx.hasPermission(permission)) {
                log.warn("Access denied: user={} missing permission={} at {}",
                    ctx.userId(), permission, pjp.getSignature().toShortString());
                throw new AccessDeniedException(
                    "User " + ctx.userId() + " does not have permission: " + permission);
            }
        }

        // Value-limit check — delegated to future ValueLimitChecker bean when needed.
        // The limit key is advisory at this stage; concrete enforcement lives in domain services.
        if (!annotation.limit().isBlank()) {
            log.trace("Value-limit key '{}' declared; concrete check delegated to domain service",
                annotation.limit());
        }

        log.trace("Permission check passed: user={} permissions={} at {}",
            ctx.userId(), Arrays.toString(requiredPermissions),
            pjp.getSignature().toShortString());

        return pjp.proceed();
    }

    /**
     * Returns the most specific {@link RequiresPermission} annotation for the
     * intercepted join point: method-level first, then class-level.
     */
    private RequiresPermission resolveAnnotation(ProceedingJoinPoint pjp) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        RequiresPermission methodAnnotation = method.getAnnotation(RequiresPermission.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return pjp.getTarget().getClass().getAnnotation(RequiresPermission.class);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 15;
    }
}
