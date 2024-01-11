import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.JavaVersion
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl

fun KotlinCompilation<KotlinCommonOptions>.configureCompilation() {
  kotlinOptions {
    freeCompilerArgs += "-Xexpect-actual-classes"
//    freeCompilerArgs += "-XXLanguage:+ExplicitBackingFields"
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

class KmpBrowserJsTargetDsl(kmpe: KotlinMultiplatformExtension) : KmpCommonTargetDsl(kmpe) {
  internal val configureJsList = mutableSetOf<KotlinJsTargetDsl.() -> Unit>()
  val js = configureJsList::add
}

fun KotlinMultiplatformExtension.kmpBrowserJsTarget(
  libs: LibrariesForLibs,
  configure: KmpBrowserJsTargetDsl.() -> Unit = {},
) {
  val dsl = KmpBrowserJsTargetDsl(this)
  dsl.configure()

  kmpCommonTarget(libs)
  js(IR) {
    browser()
    useEsModules()
    binaries.library()
    generateTypeScriptDefinitions()
    dsl.configureJsList.forEach { it() }
  }
  dsl.provides(sourceSets.jsMain, sourceSets.jsTest)
  sourceSets.jsMain.dependencies {
    implementation(libs.kotlinx.html)
    implementation(project.dependencies.enforcedPlatform(libs.kotlin.wrappers.bom))
    implementation(libs.kotlin.js)
    implementation(libs.kotlin.web)
    implementation(libs.kotlin.browser)
  }
}

class KmpNodeWasmTargetDsl(kmpe: KotlinMultiplatformExtension) : KmpCommonTargetDsl(kmpe) {
  internal val configureJsList = mutableSetOf<KotlinJsTargetDsl.() -> Unit>()
  val js = configureJsList::add
}

fun KotlinMultiplatformExtension.kmpNodeWasmTarget(
  libs: LibrariesForLibs,
  configure: KmpNodeWasmTargetDsl.() -> Unit = {}
) {
  val dsl = KmpNodeWasmTargetDsl(this)
  dsl.configure()

  kmpCommonTarget(libs)
  @Suppress("OPT_IN_USAGE")
  wasmJs {
    // 这里默认就是 useEsModules
    nodejs()
    applyBinaryen()
    binaries.library()
    dsl.configureJsList.forEach { it() }
  }
  val kotlinxCoroutinesWasmVersion = "1.8.0-RC2"
  dsl.provides(sourceSets.named("wasmJsMain"), sourceSets.named("wasmJsTest"))
  sourceSets.named("wasmJsMain") {
    dependencies {
      implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesWasmVersion")
    }
  }
  sourceSets.named("wasmJsTest") {
    dependencies {
      implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutinesWasmVersion")
    }
  }
}

//const val wasiVersion = "1.8.0-RC2-wasm0"
//
//@OptIn(ExperimentalWasmDsl::class)
//fun KotlinMultiplatformExtension.kmpWasiTarget(libs: LibrariesForLibs) {
//  wasmWasi {
//    nodejs()
//  }
//  sourceSets.named("wasmWasiMain") {
//    dependencies {
//      implementation(object : Dependency by libs.kotlinx.coroutines.core.get() {
//        override fun getVersion(): String {
//          return wasiVersion
//        }
//      })
//    }
//  }
//}
//
//fun KotlinMultiplatformExtension.kmpWasiTest(libs: LibrariesForLibs) {
//  sourceSets.named("wasmWasiTest") {
//    dependencies {
//      implementation(object : Dependency by libs.test.kotlin.coroutines.test.get() {
//        override fun getVersion(): String {
//          return wasiVersion
//        }
//      })
//      implementation(object : Dependency by libs.test.kotlin.coroutines.debug.get() {
//        override fun getVersion(): String {
//          return wasiVersion
//        }
//      })
//    }
//  }
//}
typealias WhenProvideCallback = (sourceSet: KotlinSourceSet) -> Unit

open class KmpCommonTargetDsl(private val kmpe: KotlinMultiplatformExtension) {
  internal val dependsOnList = mutableSetOf<KotlinSourceSet>()
  val dependsOn = dependsOnList::add
  internal val configureDependencyList = mutableSetOf<KotlinDependencyHandler.() -> Unit>()
  val dependencies = configureDependencyList::add
  internal val testDependsOnList = mutableSetOf<KotlinSourceSet>()
  val testDependsOn = testDependsOnList::add
  internal val configureTestDependencyList = mutableSetOf<KotlinDependencyHandler.() -> Unit>()
  val testDependencies = configureTestDependencyList::add

  companion object {
    /// TODO 这里要做成允许声明式的支持，就是不论 provides 和 whenProvides 的执行顺序，它们一定会交集在一起
    private val whenProvideCallbacks =
      mutableMapOf<KotlinMultiplatformExtension, MutableSet<WhenProvideCallback>>()
  }

  internal fun whenProvides(cb: WhenProvideCallback) = whenProvideCallbacks.get(kmpe).let {
    it ?: mutableSetOf<WhenProvideCallback>().also { whenProvideCallbacks.set(kmpe, it) }
  }.add(cb)

  internal fun provides(
    sourceSetProvider: NamedDomainObjectProvider<KotlinSourceSet>,
    testSourceSetProvider: NamedDomainObjectProvider<KotlinSourceSet>? = null,
  ) {
    val cbs = whenProvideCallbacks.getOrDefault(kmpe, mutableSetOf())
    sourceSetProvider.get().run {
      dependsOnList.forEach { dependsOn(it) }
      dependencies {
        configureDependencyList.forEach { it() }
      }
      cbs.forEach { it(this) }
    }
    testSourceSetProvider?.get()?.run {
      testDependsOnList.forEach { dependsOn(it) }
      dependencies {
        configureTestDependencyList.forEach { it() }
      }
      cbs.forEach { it(this) }
    }
  }
}

fun KotlinMultiplatformExtension.kmpCommonTarget(
  libs: LibrariesForLibs,
  configure: KmpCommonTargetDsl.() -> Unit = {}
) {
  val dsl = KmpCommonTargetDsl(this)
  dsl.configure()

  dsl.provides(sourceSets.commonMain, sourceSets.commonTest)
  sourceSets.commonMain.dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.atomicfu)
    implementation(libs.kotlinx.io)

    implementation(libs.kotlin.serialization.json)
    implementation(libs.kotlin.serialization.cbor)
  }

  sourceSets.commonTest.dependencies {
    implementation(kotlin("test"))
    implementation(libs.test.kotlin.coroutines.test)
    implementation(libs.test.kotlin.coroutines.debug)
  }
}

class KmpComposeTargetDsl(kmpe: KotlinMultiplatformExtension) : KmpCommonTargetDsl(kmpe) {
}

fun KotlinMultiplatformExtension.kmpComposeTarget(
  libs: LibrariesForLibs,
  configure: KmpComposeTargetDsl.() -> Unit = {}
) {
  val dsl = KmpComposeTargetDsl(this)
  dsl.configure()

  kmpCommonTarget(libs)
  dsl.provides(sourceSets.commonMain, sourceSets.commonTest)
  sourceSets.commonMain.dependencies {
    // jetbrains-compose-runtime
    implementation(libs.jetbrains.compose.runtime)
    implementation(libs.jetbrains.compose.foundation)
    implementation(libs.jetbrains.compose.components.resources)
//    implementation(libs.jetbrains.compose.preview)

    // jetbrains-compose-material
    implementation(libs.jetbrains.compose.material)
    implementation(libs.jetbrains.compose.material3)
    implementation(libs.jetbrains.compose.materialIcons)
  }

  dsl.whenProvides { sourceSet ->
    if (sourceSet.name == "androidMain") {
      sourceSet.dependencies {
        // AndroidX
        implementation(libs.androidx.activity)
        implementation(libs.androidx.activity.ktx)
        implementation(libs.androidx.appcompat)
        implementation(libs.androidx.animation.core.android)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.activity.compose)
      }
    }
  }
}

class KmpAndroidTargetDsl(kmpe: KotlinMultiplatformExtension) : KmpCommonTargetDsl(kmpe) {
//
//  internal val instrumentedTestDependsOnList = mutableSetOf<KotlinSourceSet>()
//  val instrumentedTestDependsOn = instrumentedTestDependsOnList::add
//  internal val configureInstrumentedTestDependencyList = mutableSetOf<KotlinDependencyHandler.() -> Unit>()
//  val instrumentedTestDependencies = configureInstrumentedTestDependencyList::add
}

fun KotlinMultiplatformExtension.kmpAndroidTarget(
  libs: LibrariesForLibs,
  configure: KmpAndroidTargetDsl.() -> Unit = {}
) {
  val dsl = KmpAndroidTargetDsl(this)
  dsl.configure()

  kmpCommonTarget(libs)
  dsl.provides(sourceSets.androidMain)
  sourceSets.androidMain.dependencies {
    implementation(libs.androidx.core.ktx)
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
}

class KmpIosTargetDsl(kmpe: KotlinMultiplatformExtension) : KmpCommonTargetDsl(kmpe) {
}

fun KotlinMultiplatformExtension.kmpIosTarget(
  libs: LibrariesForLibs,
  configure: KmpIosTargetDsl.() -> Unit = {}
) {
  val dsl = KmpIosTargetDsl(this)
  dsl.configure()

  kmpCommonTarget(libs)
  dsl.provides(sourceSets.iosMain, sourceSets.iosTest)
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
