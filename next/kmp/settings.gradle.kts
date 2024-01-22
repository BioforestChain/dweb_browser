pluginManagement {
  repositories {
    google {
      mavenContent {
        includeGroupByRegex(".*google.*")
        includeGroupByRegex(".*android.*")
      }
    }
    gradlePluginPortal()
    mavenCentral()
  }
}
plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version ("0.4.0")
}

dependencyResolutionManagement {
  repositories {
    google {
      mavenContent {
        includeGroupByRegex(".*google.*")
        includeGroupByRegex(".*android.*")
      }
    }
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
    maven("https://androidx.dev/storage/compose-compiler/repository")
  }
}

rootProject.name = "dweb-browser-kmp"

// https://docs.gradle.org/current/userguide/declaring_dependencies.html#sec:type-safe-project-accessors
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

fun includeApp(dirName: String) {
  include(dirName)
  project(":$dirName").projectDir = file("app/$dirName")
}

fun includeUI(dirName: String) {
  include("${dirName}UI")
  project(":${dirName}UI").apply {
    projectDir = file("$dirName/ui")
  }
}

include(":platformTest")
include(":platformIos")
include(":platformNode")
include(":platformBrowser")
//
include(":helper")
include(":helperCompose")
include(":helperPlatform")
include(":pureHttp")
include(":pureIO")
include(":pureCrypto")
include(":pureImage")
include(":window")
include(":core")
include(":dwebview")
include(":browser")
include(":sys")
include(":shared")
//
includeUI("pureCrypto")
includeUI("helper")
//
includeApp("androidApp")
includeApp("electronApp")
includeApp("jsFrontend")


File(rootDir, "../../toolkit/dweb_browser_libs/rust_library").eachDir { dir ->
  if (File(dir, "build.gradle.kts").exists()) {
    include(dir.name)
    project(":${dir.name}").apply {
      projectDir = file(dir)
      buildFileName = "build-mobile.gradle.kts"
    }
  }
}

fun File.eachDir(block: (File) -> Unit) {
  listFiles()?.filter { it.isDirectory }?.forEach { block(it) }
}
