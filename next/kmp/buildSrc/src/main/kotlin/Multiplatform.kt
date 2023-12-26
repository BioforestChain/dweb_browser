import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

fun KotlinCompilation<KotlinCommonOptions>.configureCompilation() {
  kotlinOptions {
    freeCompilerArgs += "-Xexpect-actual-classes"
//    freeCompilerArgs += "-Xallocator=std"
//    freeCompilerArgs += "-Xcontext-receivers"
  }
}

fun KotlinMultiplatformExtension.mobileTarget() {
  targets.all {
    compilations.all {
      configureCompilation()
    }
  }
  androidTarget {
    compilations.all {
      kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
      }
    }
  }
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_17.toString()))
  }

  iosX64()
  iosArm64()
  iosSimulatorArm64()

  applyDefaultHierarchyTemplate()
}

fun KotlinMultiplatformExtension.desktopFrontendTarget() {
  js(IR) {
    browser()
    generateTypeScriptDefinitions()
  }
}


fun KotlinMultiplatformExtension.desktopBackendTarget() {
  js(IR) {
    nodejs()
    generateTypeScriptDefinitions()
  }
}

fun Project.configureAllTests(fn: Test.() -> Unit = {}) {
  fun DependencyHandler.testImplementation(notation: Any) =
    add(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, notation)

  dependencies {
    testImplementation(platform("org.junit:junit-bom:5.7.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-launcher")
  }

  tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
      events("passed", "skipped", "failed")
    }
    fn()
  }
}

fun Project.configureNodejs() {
  plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
    // 不要下载 nodejs 可执行文件，默认环境里头已经有了
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().download =
      false
  }
}
