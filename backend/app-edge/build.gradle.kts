// app-edge: BLN (Branch Local Node) assembly — subset of modules (TDD §3.5)
// Included: identity, catalogue, inventory, sales, clinical (non-controlled),
//           procurement (receiving only), customers (credit cache),
//           reporting (branch), audit, sync-gateway (client side)
// Excluded: platform, finance-ledger, notifications-delivery,
//           consolidated reporting, any price/role/user/device administration
plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(project(":modules:shared-kernel"))
    implementation(project(":modules:identity"))
    implementation(project(":modules:catalogue"))
    implementation(project(":modules:inventory"))
    implementation(project(":modules:sales"))
    implementation(project(":modules:clinical"))
    implementation(project(":modules:procurement"))
    implementation(project(":modules:customers"))
    implementation(project(":modules:reporting"))
    implementation(project(":modules:audit"))
    implementation(project(":modules:sync-gateway"))

    // Spring Boot starters
    implementation(libs.bundles.spring.web)
    implementation(libs.bundles.spring.data)
    implementation(libs.bundles.spring.security)
    implementation(libs.bundles.observability)
    implementation(libs.springdoc.openapi.starter)

    runtimeOnly(libs.postgresql.driver)
    runtimeOnly(libs.flyway.core)
    runtimeOnly(libs.flyway.postgresql)
    runtimeOnly(libs.micrometer.prometheus)

    testImplementation(libs.bundles.testing)
    testImplementation(libs.bundles.testing.integration)
}

springBoot {
    mainClass.set("com.pharmapos.edge.PharmaPosEdgeApplication")
}

tasks.bootJar {
    archiveFileName.set("pharmapos-edge.jar")
}
