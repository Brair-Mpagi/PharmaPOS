package com.pharmapos.cloud.config;

import com.pharmapos.shared.context.TenantContextHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA configuration for auditing and tenant context integration.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

    /**
     * Provides current user ID for JPA auditing (@CreatedBy, @LastModifiedBy).
     * Extracts user ID from TenantContext.
     */
    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            if (TenantContextHolder.hasContext()) {
                return Optional.ofNullable(TenantContextHolder.getContext().getUserId());
            }
            return Optional.empty();
        };
    }
}
