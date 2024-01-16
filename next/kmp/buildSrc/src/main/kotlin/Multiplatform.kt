import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.JavaVersion
import org.gradle.api.NamedDomainObjectProvider
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
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import java.nio.file.Files

fun KotlinCompilation<KotlinCommonOptions>.configureCompilation() {
  kotlinOptions {
    freeCompilerArgs += "-Xexpect-actual-classes"
//    freeCompilerArgs += "-XXLanguage:+ExplicitBackingFields"
//    freeCompilerArgs += "-Xallocator=std"
//    freeCompilerArgs += "-Xcontext-receivers"
  }
}

open class KmpJsTargetDsl(kmpe: KotlinMultiplatformExtension) : KmpCommonTargetDsl(kmpe) {
  internal val configureJsList = mutableSetOf<KotlinJsTargetDsl.() -> Unit>()
  val js = configureJsList::add
}

class KmpBrowserJsTargetDsl(kmpe: KotlinMultiplatformExtension) : KmpJsTargetDsl(kmpe)

fun KotlinMultiplatformExtension.kmpBrowserJsTarget(
  project: Project,
  configure: KmpBrowserJsTargetDsl.() -> Unit = {},
) {
  val libs = project.the<LibrariesForLibs>()
  val dsl = KmpBrowserJsTargetDsl(this)
  dsl.configure()

  kmpCommonTarget(project)
  js("browser", IR) {
    browser {
      testTask {
        useKarma {
          useChrome()
        }
      }
    }
    useEsModules()
    binaries.library()
    generateTypeScriptDefinitions()
    dsl.configureJsList.forEach { it() }
  }

  val browserMain = sourceSets.getByName("browserMain") {
    dependencies {
      implementation(libs.kotlinx.html)
      implementation(project.dependencies.enforcedPlatform(libs.kotlin.wrappers.bom))
      implementation(libs.kotlin.js)
      implementation(libs.kotlin.web)
      implementation(libs.kotlin.browser)
    }
  }
  val browserTest = sourceSets.getByName("browserTest") {
    dependencies {
    }
  }
  dsl.provides(browserMain, browserTest)
}


class KmpNodeJsTargetDsl(kmpe: KotlinMultiplatformExtension) : KmpJsTargetDsl(kmpe)

fun KotlinMultiplatformExtension.kmpNodeJsTarget(
  project: Project,
  configure: KmpNodeJsTargetDsl.() -> Unit = {},
) {
  val libs = project.the<LibrariesForLibs>()
  val dsl = KmpNodeJsTargetDsl(this)
  dsl.configure()

  kmpCommonTarget(project)
  js("node", IR) {
    nodejs {
      testTask {
        useMocha {
        }
      }
    }
    useEsModules()
    binaries.library()
//    binaries.executable()
    generateTypeScriptDefinitions()
    dsl.configureJsList.forEach { it() }
  }
  val nodeMain = sourceSets.getByName("nodeMain") {
    dependencies {
      implementation(libs.kotlinx.html)
      implementation(project.dependencies.enforcedPlatform(libs.kotlin.wrappers.bom))
      implementation(libs.kotlin.js)
      implementation(libs.kotlin.node)
    }
  }
  val nodeTest = sourceSets.getByName("nodeTest") {
    dependencies {
    }
  }
  dsl.provides(nodeMain, nodeTest)
}

class KmpNodeWasmTargetDsl(kmpe: KotlinMultiplatformExtension) : KmpCommonTargetDsl(kmpe) {
  internal val configureJsList = mutableSetOf<KotlinJsTargetDsl.() -> Unit>()
  val js = configureJsList::add
}

fun KotlinMultiplatformExtension.kmpNodeWasmTarget(
  project: Project,
  configure: KmpNodeWasmTargetDsl.() -> Unit = {}
) {
  val libs = project.the<LibrariesForLibs>()
  val dsl = KmpNodeWasmTargetDsl(this)
  dsl.configure()

  kmpCommonTarget(project)
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

typealias KmpCommonTargetConfigure = KmpCommonTargetDsl.() -> Unit

fun KotlinSourceSet.autoLink(): KotlinSourceSet {
  if (kotlin.srcDirs.size != 1) {
    return this
  }
  val srcDir = kotlin.srcDirs.first()
  if (srcDir.exists()) {
    return this
  }
  val linkSrcDir = srcDir.resolve("../${srcDir.name}.link").normalize()
  if (!linkSrcDir.exists()) {
    val parentSrcDir = project.projectDir.parentFile.resolve("src/${name}/${srcDir.name}")
    if (!parentSrcDir.exists()) {
      return this
    }
    linkSrcDir.parentFile.mkdirs()
    Files.createSymbolicLink(
      linkSrcDir.toPath(),
      linkSrcDir.toPath().parent.relativize(parentSrcDir.toPath())
    )
  }
  kotlin.srcDirs(linkSrcDir, srcDir)
  return this
}

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

    val defaultConfigure: KmpCommonTargetConfigure = {}

  }

  internal fun whenProvides(cb: WhenProvideCallback) = whenProvideCallbacks.get(kmpe).let {
    it ?: mutableSetOf<WhenProvideCallback>().also { whenProvideCallbacks.set(kmpe, it) }
  }.add(cb)

  internal fun provides(
    sourceSet: KotlinSourceSet,
    testSourceSet: KotlinSourceSet? = null
  ) {
    val cbs = whenProvideCallbacks.getOrDefault(kmpe, mutableSetOf())
    sourceSet.run {
      autoLink()
      dependsOnList.forEach { dependsOn(it.autoLink()) }
      dependencies {
        configureDependencyList.forEach { it() }
      }
      cbs.forEach { it(this) }
    }
    testSourceSet?.run {
      autoLink()
      testDependsOnList.forEach { dependsOn(it.autoLink()) }
      dependencies {
        configureTestDependencyList.forEach { it() }
      }
      cbs.forEach { it(this) }
    }
  }

  internal fun provides(
    sourceSetProvider: NamedDomainObjectProvider<KotlinSourceSet>,
    testSourceSetProvider: NamedDomainObjectProvider<KotlinSourceSet>? = null,
  ) {
    provides(sourceSetProvider.get(), testSourceSetProvider?.get())
  }

}

val configuredCommonProjects = mutableMapOf<Project, KmpCommonTargetConfigure>()
fun KotlinDependencyHandler.implementationCommonMain(libs: LibrariesForLibs) {
  implementation(kotlin("stdlib"))
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.atomicfu)
  implementation(libs.kotlinx.io)

  implementation(libs.kotlin.serialization.json)
  implementation(libs.kotlin.serialization.cbor)
}

fun KotlinMultiplatformExtension.kmpCommonTarget(
  project: Project,
  configure: KmpCommonTargetConfigure = KmpCommonTargetDsl.defaultConfigure
) {
  if (configuredCommonProjects[project] == configure) {
    return
  }
  configuredCommonProjects[project] = configure

  val libs = project.the<LibrariesForLibs>()
  val dsl = KmpCommonTargetDsl(this)
  dsl.configure()

  dsl.provides(sourceSets.commonMain, sourceSets.commonTest)
  sourceSets.commonMain.dependencies {
    implementationCommonMain(libs)
  }

  sourceSets.commonTest.dependencies {
    implementation(kotlin("test"))
    implementation(libs.test.kotlin.coroutines.test)
    implementation(libs.test.kotlin.coroutines.debug)
  }
  applyDefaultHierarchyTemplate()

  if (project.extensions.findByName("android") != null) {
    // LibraryExtension or BaseApplicationExtension
    project.extensions.configure<com.android.build.api.dsl.CommonExtension<*, *, *, *, *>>("android") {
      namespace =
        "org.dweb_browser.${project.name.replace(Regex("[A-Z]+")) { "." + it.value.lowercase() }}"
      compileSdk = libs.versions.compileSdkVersion.get().toInt()
      defaultConfig {
        minSdk = libs.versions.minSdkVersion.get().toInt()
      }

      packaging {
        resources {
          excludes += "/META-INF/DEPENDENCIES"
        }
      }
      sourceSets.getByName("main").apply {
        res.srcDirs("src/androidMain/res", "src/commonMain/res", "src/main/res")
      }
    }
  }
}

class KmpComposeTargetDsl(kmpe: KotlinMultiplatformExtension) : KmpCommonTargetDsl(kmpe) {
}

fun KotlinMultiplatformExtension.kmpComposeTarget(
  project: Project,
  configure: KmpComposeTargetDsl.() -> Unit = {}
) {
  val libs = project.the<LibrariesForLibs>()
  val dsl = KmpComposeTargetDsl(this)
  dsl.configure()

  kmpCommonTarget(project)
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
  internal val instrumentedTestDependsOnList = mutableSetOf<KotlinSourceSet>()
  val instrumentedTestDependsOn = instrumentedTestDependsOnList::add
  internal val configureInstrumentedTestDependencyList =
    mutableSetOf<KotlinDependencyHandler.() -> Unit>()
  val instrumentedTestDependencies = configureInstrumentedTestDependencyList::add
}

fun KotlinMultiplatformExtension.kmpAndroidTarget(
  project: Project,
  configure: KmpAndroidTargetDsl.() -> Unit = {}
) {
  val libs = project.the<LibrariesForLibs>()
  val dsl = KmpAndroidTargetDsl(this)
  dsl.configure()

  kmpCommonTarget(project)
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
  project: Project,
  configure: KmpIosTargetDsl.() -> Unit = {}
) {
  val libs = project.the<LibrariesForLibs>()
  val dsl = KmpIosTargetDsl(this)
  dsl.configure()

  kmpCommonTarget(project)
  dsl.provides(sourceSets.iosMain, sourceSets.iosTest)

  targets.all {
    compilations.all {
      configureCompilation()
    }
  }
  iosX64()
  iosArm64()
  iosSimulatorArm64()
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
