import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    java
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.pit.mutation) apply false
    jacoco
}

// ---------------------------------------------------------------------------
// Global constants
// ---------------------------------------------------------------------------
val javaVersion = JavaVersion.VERSION_25

// ---------------------------------------------------------------------------
// All subprojects: Java toolchain + encoding + dependency management
// ---------------------------------------------------------------------------
subprojects {
    apply(plugin = "java")
    apply(plugin = "jacoco")
    apply(plugin = "io.spring.dependency-management")

    group = "com.pharmapos"
    version = "0.1.0-SNAPSHOT"

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-parameters", "--enable-preview"))
    }

    // Spring BOM — applies to all subprojects so dependency versions are consistent
    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:${libs.versions.springBoot.get()}")
            mavenBom("org.testcontainers:testcontainers-bom:${libs.versions.testcontainers.get()}")
        }
    }

    repositories {
        mavenCentral()
    }

    // ---------------------------------------------------------------------------
    // Common dependencies: Lombok annotation processor, UUID creator, test basics
    // ---------------------------------------------------------------------------
    dependencies {
        "compileOnly"(libs.lombok)
        "annotationProcessor"(libs.lombok)
        "annotationProcessor"(libs.mapstruct.processor)
        "implementation"(libs.uuid.creator)
        "implementation"(libs.opentelemetry.api)

        "testImplementation"(libs.bundles.testing)
        "testImplementation"(libs.spring.boot.starter.test)
        "testCompileOnly"(libs.lombok)
        "testAnnotationProcessor"(libs.lombok)
    }

    // ---------------------------------------------------------------------------
    // Test configuration — JUnit 5, Testcontainers PostgreSQL, structured logging
    // ---------------------------------------------------------------------------
    tasks.withType<Test> {
        useJUnitPlatform()
        jvmArgs("--enable-preview")
        maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
        testLogging {
            events = setOf(
                TestLogEvent.PASSED,
                TestLogEvent.FAILED,
                TestLogEvent.SKIPPED,
                TestLogEvent.STANDARD_ERROR
            )
            exceptionFormat = TestExceptionFormat.FULL
            showStandardStreams = false
        }
        systemProperty("spring.datasource.url",
            System.getenv("TEST_DATASOURCE_URL")
                ?: "jdbc:tc:postgresql:16:///pharmapos_test?TC_REUSABLE=true")
        systemProperty("spring.datasource.username", "app_rw")
        systemProperty("spring.datasource.password", "app_rw")
    }

    // ---------------------------------------------------------------------------
    // JaCoCo — reports per module
    // ---------------------------------------------------------------------------
    tasks.jacocoTestReport {
        dependsOn(tasks.test)
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}

// ---------------------------------------------------------------------------
// Aggregate coverage — 80 % line coverage enforced on critical modules
// ---------------------------------------------------------------------------
val criticalModules = setOf(
    ":modules:inventory",
    ":modules:shared-kernel",
    ":modules:identity",
    ":modules:sales",
    ":modules:audit"
)

// Register a single aggregated JaCoCo verification task across critical modules
tasks.register("verifyCriticalCoverage") {
    group = "verification"
    description = "Fails the build if critical modules fall below 80% line coverage"

    dependsOn(subprojects
        .filter { it.path in criticalModules }
        .map { "${it.path}:jacocoTestReport" })

    doLast {
        val threshold = 0.80
        subprojects
            .filter { it.path in criticalModules }
            .forEach { sub ->
                val reportFile = sub.file("build/reports/jacoco/test/jacocoTestReport.xml")
                if (!reportFile.exists()) {
                    throw GradleException("JaCoCo report missing for ${sub.path}")
                }
                val xml = reportFile.readText()
                val missedPattern = Regex("""<counter type="LINE" missed="(\d+)" covered="(\d+)"/>""")
                val (missed, covered) = missedPattern.findAll(xml).toList().last().destructured
                val lineRate = covered.toDouble() / (missed.toDouble() + covered.toDouble())
                if (lineRate < threshold) {
                    throw GradleException(
                        "${sub.path} line coverage ${(lineRate * 100).toInt()}% < required 80%"
                    )
                }
                logger.lifecycle("✅ ${sub.path} line coverage: ${(lineRate * 100).toInt()}%")
            }
    }
}

// ---------------------------------------------------------------------------
// PIT mutation testing on critical modules
// ---------------------------------------------------------------------------
subprojects {
    if (path in criticalModules) {
        apply(plugin = "info.solidsoft.pitest")

        configure<info.solidsoft.gradle.pitest.PitestPluginExtension> {
            junit5PluginVersion.set(libs.versions.pitJunit5.get())
            targetClasses.set(setOf("com.pharmapos.*"))
            targetTests.set(setOf("com.pharmapos.*"))
            mutators.set(setOf("STRONGER"))
            outputFormats.set(setOf("XML", "HTML"))
            failWhenNoMutations.set(false)
            threads.set(Runtime.getRuntime().availableProcessors())
            mutationThreshold.set(70)
        }
    }
}

// ---------------------------------------------------------------------------
// Spring Modulith boundary verification task
// The check runs as part of the app-cloud test phase via @ApplicationModuleTest
// but we also expose it here for explicit CI invocation.
// ---------------------------------------------------------------------------
tasks.register("verifyModuleBoundaries") {
    group = "verification"
    description = "Runs Spring Modulith structural verification — fails on forbidden cross-module dependency or cycle"
    dependsOn(":app-cloud:test")
}

tasks.register("check") {
    dependsOn("verifyCriticalCoverage", "verifyModuleBoundaries")
}
