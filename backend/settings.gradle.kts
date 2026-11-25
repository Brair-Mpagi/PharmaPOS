rootProject.name = "pharmapos-backend"

// Gradle 9+ auto-detects gradle/libs.versions.toml
// No need for explicit versionCatalogs block

// Core modules (bounded contexts)
include(":modules:shared-kernel")
include(":modules:catalogue")
include(":modules:inventory")
include(":modules:sales")
include(":modules:audit")

// Commented out - not implemented yet
// include(":modules:platform")
// include(":modules:identity")
// include(":modules:clinical")
// include(":modules:procurement")
// include(":modules:finance")
// include(":modules:customers")
// include(":modules:reporting")
// include(":modules:notifications")
// include(":modules:sync-gateway")

// Assembly modules
include(":app-cloud")
include(":app-edge")
