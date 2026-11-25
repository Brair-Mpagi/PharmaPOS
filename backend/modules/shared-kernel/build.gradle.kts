plugins {
    java
}

dependencies {
    api("com.github.f4b6a3:uuid-creator:6.0.0")
    api("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
}
