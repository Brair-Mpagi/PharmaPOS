// shared-kernel: pure domain value objects — no Spring Boot, no JPA
plugins {
    java
}

dependencies {
    implementation(libs.jackson.databind)
    implementation(libs.uuid.creator)
    testImplementation(libs.bundles.testing)
}
