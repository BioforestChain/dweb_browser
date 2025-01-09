@file:OptIn(ExperimentalKotlinGradlePluginApi::class)
@file:Suppress("UNUSED_VARIABLE")

import com.android.build.gradle.internal.lint.AndroidLintAnalysisTask
import com.android.build.gradle.internal.lint.LintModelWriterTask
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.JavaVersion
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest
import java.io.File
import java.nio.file.Files
import java.util.Properties

fun KotlinCompilation<*>.configureCompilation() {
  compileTaskProvider.configure {
    compilerOptions {
      freeCompilerArgs.addAll(
        "-Xexpect-actual-classes",
        "-Xcontext-receivers",
        "-XXLanguage:+ExplicitBackingFields",
      )
    }
  }
}

open class KmpJsTargetDsl(kmpe: KotlinMultiplatformExtension) : KmpBaseTargetDsl(kmpe) {
  internal val configureJsList = mutableSetOf<KotlinJsTargetDsl.() -> Unit>()
  val js = configureJsList::add
}

class KmpBrowserJsTargetDsl(kmpe: KotlinMultiplatformExtension) : KmpJsTargetDsl(kmpe)

val KotlinMultiplatformExtension.compose: ComposePlugin.Dependencies
  get() = (this as ExtensionAware).extensions.getByName("compose") as ComposePlugin.Dependencies

fun KotlinMultiplatformExtension.kmpBrowserJsTarget(
  project: Project,
  configure: KmpBrowserJsTargetDsl.() -> Unit = {},
) {
  if (Features.electronApp.disabled) {
    return
  }
  println("kmpBrowserJsTarget: ${project.name}")

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
      implementationProject("platformBrowser")
    }
  }
  val browserTest = sourceSets.getByName("browserTest") {
    dependencies {
    }
  }
  dsl.provides(browserMain, browserTest)
}

fun RegularFileProperty.fixSuffix(isEsm: Boolean) {
  val inputFilePath = get().asFile.absolutePath
  if (isEsm) {
    if (inputFilePath.endsWith(".js")) {
      println("update js suffix(.js -> .mjs)")
      fileValue(
        File(inputFilePath.replace(Regex("\\.js$"), ".mjs"))
      )
    }
  } else {
    if (inputFilePath.endsWith(".mjs")) {
      println("update js suffix(.mjs -> .js)")
      fileValue(
        File(inputFilePath.replace(Regex("\\.mjs$"), ".js"))
      )
    }
  }
}

class KmpNodeWasmTargetDsl(kmpe: KotlinMultiplatformExtension) : KmpBaseTargetDsl(kmpe) {
  internal val configureJsList = mutableSetOf<KotlinJsTargetDsl.() -> Unit>()
  val js = configureJsList::add
}

fun KotlinMultiplatformExtension.kmpNodeWasmTarget(
  project: Project,
  configure: KmpNodeWasmTargetDsl.() -> Unit = {},
) {
  val libs = project.the<LibrariesForLibs>()
  val dsl = KmpNodeWasmTargetDsl(this)
  dsl.configure()

  kmpCommonTarget(project)
  @Suppress("OPT_IN_USAGE")
  wasmJs {
    // 这里默认就是 useEsModules
    nodejs()
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

open class KmpBaseTargetDsl(private val kmpe: KotlinMultiplatformExtension) {
  val features = Features
  val platform = Platform
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
    sourceSet: KotlinSourceSet,
    testSourceSet: KotlinSourceSet? = null,
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
//      testDependencies {
//        configureTestDependencyList.forEach { it() }
//      }
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

fun KotlinDependencyHandler.implementationProject(projectName: String) {
  if (project.name != projectName) {
    implementation(project(":$projectName"))
  }
}

val configuredCommonProjects = mutableMapOf<Project, KmpCommonTargetConfigure>()

class KmpCommonTargetDsl(kmpe: KotlinMultiplatformExtension) : KmpBaseTargetDsl(kmpe) {
  companion object {
    val defaultConfigure: KmpCommonTargetConfigure = {}
  }
}

fun KotlinHierarchyBuilder.withIosTarget() {
  if (Features.iosApp.disabled || !Platform.isMac) {
    return
  }
  group("native") {
    withIos()
  }
}

fun KotlinHierarchyBuilder.withDesktopTarget() {
  withJvm()
}

fun KotlinMultiplatformExtension.applyHierarchyPlatformTemplate(template: KotlinHierarchyBuilder.Root.() -> Unit) =
  if (Platform.isMac) applyDefaultHierarchyTemplate(template) else applyHierarchyTemplate(template)

fun KotlinMultiplatformExtension.kmpCommonTarget(
  project: Project,
  kspSrcDirs: (SourceDirectorySet.() -> Unit)? = null,
  configure: KmpCommonTargetConfigure = KmpCommonTargetDsl.defaultConfigure,
) {
  if (configuredCommonProjects[project].let {
      configure == it || (configure == KmpCommonTargetDsl.defaultConfigure && it != null)
    }
  ) {
    return
  }
  configuredCommonProjects[project] = configure

  val libs = project.the<LibrariesForLibs>()
  val dsl = KmpCommonTargetDsl(this)
  dsl.configure()

  dsl.provides(sourceSets.commonMain, sourceSets.commonTest)
  sourceSets.all {
    kotlin {
      languageSettings.enableLanguageFeature("ExplicitBackingFields")
    }
  }

  sourceSets.commonMain {
    if (kspSrcDirs != null) {
      kotlin.kspSrcDirs()
    }

    dependencies {
      implementation(libs.kotlinx.coroutines.core)
      implementation(libs.kotlinx.atomicfu)
      implementation(libs.kotlinx.io)

      implementation(libs.kotlin.serialization.json)
      implementation(libs.kotlin.serialization.cbor)
    }
  }

  sourceSets.commonTest.dependencies {
    implementation(kotlin("test"))
    implementation(libs.test.kotlin.coroutines.test)
    implementation(libs.test.kotlin.coroutines.debug)
    implementationProject("platformTest")
  }
  targets.all {
    compilations.all {
      configureCompilation()
    }
  }

  if (project.extensions.findByName("android") != null) {
    // LibraryExtension or BaseApplicationExtension
    project.extensions.configure<com.android.build.api.dsl.CommonExtension<*, *, *, *, *, *>>("android") {
      namespace =
        "org.dweb_browser.${project.name.replace(Regex("[A-Z]+")) { (if (it.range.first == 0) "" else ".") + it.value.lowercase() }}"
      println("namespace: $namespace")
      compileSdk = libs.versions.compileSdkVersion.get().toInt()
      defaultConfig {
        minSdk = libs.versions.minSdkVersion.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
      }

      packaging {
        resources {
          excludes += "/META-INF/DEPENDENCIES"
        }
      }
      sourceSets.getByName("main").apply {
        res.srcDirs("src/androidMain/res", "src/commonMain/res", "src/main/res")
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
      }
    }
  }

  project.tasks.withType<Copy> {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
  }
}

class KmpComposeTargetDsl(kmpe: KotlinMultiplatformExtension) : KmpBaseTargetDsl(kmpe) {
}

fun KotlinMultiplatformExtension.kmpComposeTarget(
  project: Project,
  configure: KmpComposeTargetDsl.() -> Unit = {},
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
    implementation(libs.jetbrains.compose.ui)
    implementation(libs.jetbrains.compose.components.resources)

    // jetbrains-compose-material
    implementation(libs.jetbrains.compose.material)
    implementation(libs.jetbrains.compose.material3)
    implementation(libs.jetbrains.compose.materialIcons)
    implementation(libs.jetbrains.androidx.navigation)

    implementation(compose.components.uiToolingPreview)
  }
  sourceSets.commonTest.dependencies {
    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
    implementation(compose.uiTest)
//    implementation(compose.desktop.uiTestJUnit4)
//    implementation(libs.test.junit.jupiter)
//    implementation(libs.test.junit.jupiter.engine)
  }

  dsl.whenProvides { sourceSet ->
    when (sourceSet.name) {
      "androidMain" -> sourceSet.dependencies {
        // AndroidX
        implementation(libs.androidx.activity)
        implementation(libs.androidx.activity.ktx)
        implementation(libs.androidx.appcompat)
        implementation(libs.androidx.animation.core.android)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.activity.compose)

//        implementation(libs.androidx.adaptive)
//        implementation(libs.androidx.adaptive.layout)
//        implementation(libs.androidx.adaptive.navigation)
        implementation(libs.compose.ui.tooling)
        implementation(libs.compose.ui.tooling.preview)
      }

      "androidTest" -> sourceSet.dependencies {
      }

      "androidDebug" -> sourceSet.dependencies {
      }

      "androidInstrumentedTest" -> sourceSet.dependencies {
        implementation(compose.desktop.uiTestJUnit4)
        implementation(libs.compose.ui.test.manifest)
      }

      "desktopMain" -> sourceSet.dependencies {
        implementation(libs.jetbrains.compose.ui.tooling)
        implementation(libs.jetbrains.compose.ui.tooling.preview.desktop)
      }

      "desktopTest" -> sourceSet.dependencies {
        // Adds the desktop test dependency
        implementation(compose.desktop.currentOs)
        implementation(compose.desktop.uiTestJUnit4)
      }
    }
  }
}

class KmpAndroidTargetDsl(kmpe: KotlinMultiplatformExtension) : KmpBaseTargetDsl(kmpe) {
  internal val instrumentedTestDependsOnList = mutableSetOf<KotlinSourceSet>()
  val instrumentedTestDependsOn = instrumentedTestDependsOnList::add
  internal val configureInstrumentedTestDependencyList =
    mutableSetOf<KotlinDependencyHandler.() -> Unit>()
  val instrumentedTestDependencies = configureInstrumentedTestDependencyList::add

}

fun KotlinMultiplatformExtension.kmpAndroidTarget(
  project: Project,
  forceEnable: Boolean = false,
  configure: KmpAndroidTargetDsl.() -> Unit = {},
) {
  if (Features.androidApp.disabled && !forceEnable) {
    return
  }
  println("kmpAndroidTarget: ${project.name}")

  val dsl = KmpAndroidTargetDsl(this)
  dsl.configure()
  val libs = project.the<LibrariesForLibs>()

  kmpCommonTarget(project)
  dsl.provides(sourceSets.androidMain, sourceSets.androidUnitTest)
  sourceSets.androidMain.dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.annotation)
  }
  sourceSets.androidUnitTest.dependencies {
    implementationProject("platformTest")
  }
  androidTarget {
    compilations.all {
      compileTaskProvider.configure {
        compilerOptions {
          jvmTarget.set(JvmTarget.JVM_17)
        }
      }
    }
  }
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_17.toString()))
  }
  if (project.tasks.findByName("copyFontsToAndroidAssets") != null) {
    project.tasks.withType<LintModelWriterTask> {
      dependsOn("copyFontsToAndroidAssets")
    }
    project.tasks.withType<AndroidLintAnalysisTask> {
      dependsOn("copyFontsToAndroidAssets")
    }
  }
}

class KmpDesktopTargetDsl(kmpe: KotlinMultiplatformExtension) : KmpBaseTargetDsl(kmpe)

fun KotlinMultiplatformExtension.kmpDesktopTarget(
  project: Project,
  configure: KmpDesktopTargetDsl.() -> Unit = {},
) {
  println("Features.desktopApp.disabled=${Features.desktopApp.disabled}")
  if (Features.desktopApp.disabled) {
    return
  }

  println("KmpDesktopTarget: ${project.name}")

  val dsl = KmpDesktopTargetDsl(this)
  dsl.configure()
  val libs = project.the<LibrariesForLibs>()

  kmpCommonTarget(project)
  jvm("desktop")
  val desktopMain = sourceSets.getByName("desktopMain")
  val desktopTest = sourceSets.getByName("desktopTest")
  dsl.provides(desktopMain, desktopTest)
  desktopMain.dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.annotation)
    // 一定要加 https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.1.1
    implementation(libs.kotlinx.coroutines.swing)

    implementationProject("platformDesktop")
  }
  desktopTest.dependencies {
    implementationProject("platformTest")
  }
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_17.toString()))
  }
}

class KmpIosTargetDsl(kmpe: KotlinMultiplatformExtension) : KmpBaseTargetDsl(kmpe) {
}

fun KotlinMultiplatformExtension.kmpIosTarget(
  project: Project,
  configure: KmpIosTargetDsl.() -> Unit = {},
) {
  if (Features.iosApp.disabled || !Platform.isMac) {
    return
  }
  println("kmpIosTarget: ${project.name}")

  val dsl = KmpIosTargetDsl(this)
  dsl.configure()
  val libs = project.the<LibrariesForLibs>()

  kmpCommonTarget(project)
  sourceSets.iosMain.dependencies {
    implementationProject("platformIos")
  }
  sourceSets.iosTest.dependencies {
    implementationProject("platformTest")
  }
  dsl.provides(sourceSets.iosMain, sourceSets.iosTest)

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

// 用于启动桌面应用时注入key
  afterEvaluate {
    tasks.withType<JavaExec>() {
      localProperties().copyTo(systemProperties)
      System.getProperties().copyTo(systemProperties)
//    jvmArgs("--add-opens", "java.desktop/java.awt=ALL-UNNAMED")
    }
    tasks.withType<KotlinJvmTest>() {
      localProperties().copyTo(systemProperties)
      System.getProperties().copyTo(systemProperties)
    }
  }
}

infix fun String.belong(domain: String) = this == domain || this.startsWith("$domain.")

fun Project.getJxBrowserLicenseKey() = System.getProperty("jxbrowser.license.key")
  ?: localProperties().getProperty("jxbrowser.license.key", "")

private val Project.propertiesMap by lazy { mutableMapOf<String, Properties>() }
fun Project.localProperties(filename: String = "local.properties"): Properties {
  return propertiesMap.getOrPut(filename) {
    Properties().also { properties ->
      rootDir.resolve("local.properties").apply {
        if (exists()) {
          inputStream().use { properties.load(it) }
        }
      }
    }
  }
}

fun Properties.getBoolean(key: String) = this[key] == "true"
fun Properties.getString(key: String) =
  (this[key] ?: throw Exception("properties key not found: $key")).toString()

fun Properties.getStringOrNull(key: String) = this[key]?.toString()

fun Properties.copyTo(outProperties: MutableMap<String, Any>) {
  for ((key, value) in this) {
    if (key is String) {
      if (key == "jxbrowser.license.key" || key.startsWith("dweb-")) {
        outProperties[key] = value
      }
    }
  }
}