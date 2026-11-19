package com.pharmapos.cloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.modulith.Modulith;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * PharmaPOS Cloud Main Application.
 * 
 * This is the cloud-hosted back-office deployment of the modular monolith.
 * It includes all modules except BLN-specific edge functionality.
 * 
 * Architecture:
 * - Spring Modulith enforces module boundaries
 * - Multi-tenant with Row-Level Security
 * - Event-driven via transactional outbox
 * - Keycloak OIDC authentication
 */
@SpringBootApplication(scanBasePackages = {
    "com.pharmapos.sharedkernel",
    "com.pharmapos.catalogue",
    "com.pharmapos.inventory",
    "com.pharmapos.sales",
    "com.pharmapos.audit",
    "com.pharmapos.cloud"
})
@EnableJpaAuditing
@ConfigurationPropertiesScan("com.pharmapos")
public class PharmaPosCloudApplication {

    public static void main(String[] args) {
        SpringApplication.run(PharmaPosCloudApplication.class, args);
    }
}
