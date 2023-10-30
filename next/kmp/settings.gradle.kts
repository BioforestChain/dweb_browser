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
include("androidApp")
project(":androidApp").projectDir = file("app/androidApp")
//include("desktopApp")
//project(":desktopApp").projectDir = file("app/desktopApp")

include(":shared")
include(":helper")
include(":helperCompose")
include(":helperPlatform")
include(":window")
include(":core")
include(":DWebView")
include(":browser")
include(":mdns")
include("ziplib")
project(":ziplib").projectDir = file("../../toolkit/dweb_browser_libs/rust_library/ziplib")
include(":sys")
