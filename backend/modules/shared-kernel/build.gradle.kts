plugins {
    java
}

dependencies {
    api(libs.uuid.creator)
    api(libs.jackson.databind)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
}
