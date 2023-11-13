pluginManagement {
  repositories {
    google()
    gradlePluginPortal()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
  }
}
plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version ("0.4.0")
}

dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
  }
}

rootProject.name = "dweb-browser-kmp"

// https://docs.gradle.org/current/userguide/declaring_dependencies.html#sec:type-safe-project-accessors
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include("androidApp")
project(":androidApp").projectDir = file("app/androidApp")
//include("desktopApp")
//project(":desktopApp").projectDir = file("app/desktopApp")

include(
  ":shared",
  ":helper",
  ":helperCompose",
  ":helperPlatform",
  ":window",
  ":core",
  ":DWebView",
  ":browser",
  ":sys"
)
include("ziplib")
project(":ziplib").projectDir = file("../../toolkit/dweb_browser_libs/rust_library/ziplib")
include("reverse_proxy")
project(":reverse_proxy").projectDir = file("../../toolkit/dweb_browser_libs/rust_library/reverse_proxy")
