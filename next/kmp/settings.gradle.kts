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
//apply(from = "gradle/features.gradle.kts")
/**
 * 好像无法与buildSrc共享代码，但是gradle官方却有相关的例子，只不过这种共享好像达不到我们的需求
 * https://github.com/gradle/gradle/blob/master/settings.gradle.kts#L38C1-L38C13
 */
class FeaturesFactory {
  private val props = java.util.Properties().also { properties ->
    rootDir.resolve("local.properties").apply {
      if (exists()) {
        inputStream().use { properties.load(it) }
      }
    }
  }

  private val disabled = props.getProperty("app.disable", "")
    .split(",")
    .map { it.trim().lowercase() };

  private val enabled = props.getProperty("app.experimental.enabled", "")
    .split(",")
    .map { it.trim().lowercase() };

  class Bool(val enabled: Boolean) {
    val disabled = !enabled
  }

  val androidApp = Bool(!disabled.contains("android"));
  val iosApp = Bool(!disabled.contains("ios"));
  val desktopApp = Bool(enabled.contains("desktop"));
  val electronApp = Bool(enabled.contains("electron"));
  val libs = Bool(androidApp.enabled || iosApp.enabled || desktopApp.enabled)

  init {
    println("androidApp.enabled=${androidApp.enabled}")
    println("iosApp.enabled=${iosApp.enabled}")
    println("desktopApp.enabled=${desktopApp.enabled}")
    println("electronApp.enabled=${electronApp.enabled}")
    println("libs.enabled=${libs.enabled}")
  }
}

val features = FeaturesFactory()

rootProject.name = "dweb-browser-kmp"
System.setProperty("dweb-browser.root.dir", rootDir.absolutePath)

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
if (features.iosApp.enabled) {
  include(":platformIos")
}
if (features.electronApp.enabled) {
  include(":platformNode")
  include(":platformBrowser")
}
if (features.desktopApp.enabled) {
  include(":platformDesktop")
}

if (features.libs.enabled) {
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
if (features.androidApp.enabled) {
  includeApp("androidApp")
  includeApp("androidBenchmark")
}
if (features.electronApp.enabled) {
  includeApp("jsCommon")
  includeApp("electronApp")
  includeApp("jsFrontend")
//  includeApp("demoReactApp")
  includeApp("demoComposeApp")
}

if (features.desktopApp.enabled) {
  includeApp("desktopApp")
}

if (features.libs.enabled) {
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

