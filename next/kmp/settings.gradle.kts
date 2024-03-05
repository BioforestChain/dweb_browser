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
    maven("https://us-maven.pkg.dev/jxbrowser/releases")
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

val properties = java.util.Properties().also { properties ->
  file("local.properties").apply {
    if (exists()) {
      inputStream().use { properties.load(it) }
    }
  }
}
val disabledApps = (properties.getOrDefault("app.disable", "") as String)
  .split(",")
  .map { it.trim().lowercase() }
val enableAndroidApp = !disabledApps.contains("android")
val enableIosApp = !disabledApps.contains("ios")
val enableElectronApp = !disabledApps.contains("electron")
val enableDesktop = !disabledApps.contains("desktop")
val enableLibs = enableAndroidApp || enableIosApp || enableDesktop

include(":platformTest")
if (enableIosApp) {
  include(":platformIos")
}
if (enableElectronApp) {
  include(":platformNode")
  include(":platformBrowser")
}
if (enableDesktop) {
  include(":platformDesktop")
}

if(enableLibs){
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
}
//includeUI("pureCrypto")
//includeUI("helper")
if (enableAndroidApp) {
  includeApp("androidApp")
  includeApp("androidBenchmark")
}
if (enableElectronApp) {
  includeApp("jsCommon")
  includeApp("electronApp")
  includeApp("jsFrontend")
//  includeApp("demoReactApp")
  includeApp("demoComposeApp")
}

if(enableLibs){
  File(
    rootDir,
    "../../toolkit/dweb_browser_libs/rust_library"
  ).listFiles { file -> file.isDirectory }
    ?.forEach { dir ->
      if (File(dir, "build.gradle.kts").exists()) {
        include(dir.name)
        project(":${dir.name}").apply {
          name = "lib_${dir.name}"
          projectDir = file(dir)
          buildFileName = "build-mobile.gradle.kts"
        }
      }
    }
}

