import org.gradle.accessors.dm.LibrariesForLibs
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

fun KotlinMultiplatformExtension.kmpMobileTarget() {
  kmpAndroidTarget()
  kmpIosTarget()
}

fun KotlinMultiplatformExtension.kmpAndroidTarget() {
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
}

fun KotlinMultiplatformExtension.kmpIosTarget() {
  applyDefaultHierarchyTemplate()
  targets.all {
    compilations.all {
      configureCompilation()
    }
  }
  iosX64()
  iosArm64()
  iosSimulatorArm64()
}


enum class JsPlatform {
  Browser,
  Node,
  Electron,
  ;
}


fun KotlinMultiplatformExtension.kmpJsTarget(libs: LibrariesForLibs, vararg platforms: JsPlatform) {
  js(IR) {
    if (platforms.contains(JsPlatform.Browser)) {
      browser()
    }
    if (platforms.contains(JsPlatform.Node)) {
      nodejs()
    }
    generateTypeScriptDefinitions()
  }

  sourceSets.jsMain.dependencies {
    implementation(project.dependencies.enforcedPlatform(libs.kotlin.wrappers.bom))
    implementation(libs.kotlin.js)
    if (platforms.contains(JsPlatform.Browser)) {
      implementation(libs.kotlin.web)
      implementation(libs.kotlin.browser)
    }
    if (platforms.contains(JsPlatform.Node)) {
      implementation(libs.kotlin.node)
    }
    if (platforms.contains(JsPlatform.Electron)) {
      implementation(libs.kotlin.electron)
    }
  }
}

fun Project.configureJvmTests(fn: Test.() -> Unit = {}) {
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
