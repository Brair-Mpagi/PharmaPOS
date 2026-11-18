plugins {
    java
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(project(":modules:shared-kernel"))
    implementation(libs.bundles.spring.web)
    implementation(libs.bundles.spring.data)
    implementation(libs.bundles.spring.modulith)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.springdoc.openapi.starter)
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
    runtimeOnly(libs.postgresql.driver)
    runtimeOnly(libs.flyway.core)
    runtimeOnly(libs.flyway.postgresql)
    implementation(project(":modules:catalogue"))
    testImplementation(libs.bundles.testing)
    testImplementation(libs.bundles.testing.integration)
    testImplementation(libs.spring.modulith.test)
    testImplementation(libs.archunit.junit5)
}
