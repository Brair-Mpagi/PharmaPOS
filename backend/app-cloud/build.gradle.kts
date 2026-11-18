// app-cloud: Cloud Core assembly — all bounded-context modules
plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    // All bounded-context modules
    implementation(project(":modules:shared-kernel"))
    implementation(project(":modules:platform"))
    implementation(project(":modules:identity"))
    implementation(project(":modules:catalogue"))
    implementation(project(":modules:inventory"))
    implementation(project(":modules:sales"))
    implementation(project(":modules:clinical"))
    implementation(project(":modules:procurement"))
    implementation(project(":modules:finance"))
    implementation(project(":modules:customers"))
    implementation(project(":modules:reporting"))
    implementation(project(":modules:notifications"))
    implementation(project(":modules:sync-gateway"))
    implementation(project(":modules:audit"))

    // Spring Boot starters
    implementation(libs.bundles.spring.web)
    implementation(libs.bundles.spring.data)
    implementation(libs.bundles.spring.security)
    implementation(libs.bundles.observability)
    implementation(libs.spring.modulith.actuator)
    implementation(libs.springdoc.openapi.starter)

    runtimeOnly(libs.postgresql.driver)
    runtimeOnly(libs.flyway.core)
    runtimeOnly(libs.flyway.postgresql)
    runtimeOnly(libs.micrometer.prometheus)

    testImplementation(libs.bundles.testing)
    testImplementation(libs.bundles.testing.integration)
    testImplementation(libs.spring.modulith.test)
    testImplementation(libs.archunit.junit5)
}

springBoot {
    mainClass.set("com.pharmapos.cloud.PharmaPosCloudApplication")
}

tasks.bootJar {
    archiveFileName.set("pharmapos-cloud.jar")
}
