rootProject.name = "pharmapos-backend"

// Version catalog
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}

// Core modules (bounded contexts)
include(":modules:shared-kernel")
include(":modules:platform")
include(":modules:identity")
include(":modules:catalogue")
include(":modules:inventory")
include(":modules:sales")
include(":modules:clinical")
include(":modules:procurement")
include(":modules:finance")
include(":modules:customers")
include(":modules:reporting")
include(":modules:notifications")
include(":modules:sync-gateway")
include(":modules:audit")

// Assembly modules
include(":app-cloud")
include(":app-edge")
