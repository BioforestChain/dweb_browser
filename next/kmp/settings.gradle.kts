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
  buildscript {
    repositories {
      mavenCentral()
      maven {
        url = uri("https://storage.googleapis.com/r8-releases/raw")
      }
      maven {
        url = uri("https://plugins.gradle.org/m2/")
      }
    }
    dependencies {
      classpath("com.android.tools:r8:8.9.27") // R8 是 Android 构建工具链的一部分，用于替代 ProGuard ： 代码缩减/类重命名和优化/性能优化
    }
  }
}
plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version ("0.9.0")
}

@Suppress("UnstableApiUsage")
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
    /**
     * 北美 的仓库地址
     * https://github.com/TeamDev-IP/JxBrowser-Gradle-Plugin/blob/main/src/main/kotlin/com/teamdev/jxbrowser/gradle/Repository.kt
     */
    maven("https://us-maven.pkg.dev/jxbrowser/releases")
    maven("https://europe-maven.pkg.dev/jxbrowser/eaps")
  }
}
//apply(from = "gradle/features.gradle.kts")
/**
 * 好像无法与buildSrc共享代码，但是gradle官方却有相关的例子，只不过这种共享好像达不到我们的需求
 * https://github.com/gradle/gradle/blob/master/settings.gradle.kts#L38C1-L38C13
 */
class PlatformFactory {
  val osName = System.getProperty("os.name")
  val osArch = System.getProperty("os.arch")

  init {
    println("platform-os-name=$osName")
    println("platform-os-arch=$osArch")
  }

  val isMac = osName.startsWith("Mac")
  val isWindows = osName.startsWith("Windows")
  val isLinux = osName.startsWith("Linux")

  /// 寄存器宽度
  val is64 = listOf("amd64", "x86_64", "aarch64").contains(osArch)
  val is32 = listOf("x86", "arm").contains(osArch)

  /// 指令集
  val isX86 = setOf("x86", "i386", "amd64", "x86_64").contains(osArch)
  val isArm = listOf("aarch64", "arm").contains(osArch)
}

val platform = PlatformFactory()

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
  val iosApp = Bool(platform.isMac && !disabled.contains("ios"));
  val desktopApp = Bool(!disabled.contains("desktop"));
  val electronApp = Bool(enabled.contains("electron"));
  val extlibs = Bool(androidApp.enabled || iosApp.enabled || desktopApp.enabled)

  init {
    println("androidApp.enabled=${androidApp.enabled}")
    println("iosApp.enabled=${iosApp.enabled}")
    println("desktopApp.enabled=${desktopApp.enabled}")
    println("electronApp.enabled=${electronApp.enabled}")
    println("libs.enabled=${extlibs.enabled}")
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

fun includePinpitApp(dirName: String) {
  include(dirName)
  project(":${dirName}").apply {
    projectDir = file("app/$dirName")
    buildFileName = "build-pinpit.gradle.kts"
  }
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
if (features.desktopApp.enabled) {
  include(":platformDesktop")
}

if (features.extlibs.enabled) {
  include(":helper")
  include(":helperCompose")
  include(":helperCapturable")
  include(":helperPlatform")
  include(":helperKsp")

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
  includeApp("androidBaselineprofile")
}

if (features.desktopApp.enabled) {
  val usePinpit = System.getProperty("usePinpit")

  if (usePinpit == "true") {
    includePinpitApp("desktopApp")
  } else {
    includeApp("desktopApp")
  }
}

