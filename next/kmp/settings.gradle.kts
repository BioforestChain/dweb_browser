pluginManagement {
  repositories {
    google()
    gradlePluginPortal()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
  }
}

dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
  }
}

rootProject.name = "dweb-browser"
include("androidApp")
project(":androidApp").projectDir = file("app/androidApp")
include("desktopApp")
project(":desktopApp").projectDir = file("app/desktopApp")

include(":shared")
include(":helper")
include(":helperCompose")
include(":helperPlatform")
include(":window")
include(":microService")

