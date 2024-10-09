
import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  id("target-common")
}


val libs = the<LibrariesForLibs>()

kotlin {
  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  compilerOptions {
    languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
    apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
  }
  kmpCommonTarget(project) {
    dependencies {
      implementation(libs.squareup.okio)
      implementation(libs.kotlinx.datetime)
    }
  }
  kmpIosTarget(project) {
    val project_base_name = project.name.replace(Regex("^lib_"), "")
    listOf(
      iosArm64(),
      iosSimulatorArm64()
    ).forEach {
      it.binaries.framework {
        baseName = project_base_name
      }
      val main by it.compilations.getting
      main.cinterops.create(project_base_name) {
        includeDirs(
          project.file("src/nativeInterop/cinterop/headers/$project_base_name"),
          project.file("src/libs/${it.targetName}")
        )
      }
    }
  }
  kmpDesktopTarget(project) {
    dependencies {
      api(libs.java.jna)
    }
  }
  sourceSets.all {
    languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
  }

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  applyHierarchyPlatformTemplate {
    common {
      withDesktopTarget()
      withIosTarget()
    }
  }
}
