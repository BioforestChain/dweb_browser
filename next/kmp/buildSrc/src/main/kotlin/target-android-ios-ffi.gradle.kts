import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  id("target-common")
  id("target-android")
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

  // 因为至少需要一个 target，所以如果lib被禁用，就 android 的强制开起来
  // 不可以直接禁用这个 module，这样会使得 libs.projects.lib* 找不到目标
  kmpAndroidTarget(project, Features.libs.disabled) {
    dependencies {
      api(libs.java.jna.map {
        project.dependencies.create(it, closureOf<ExternalModuleDependency> {
          artifact {
            type = "aar"
          }
        })
      })
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
  sourceSets.all {
    languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
  }
}


android {
  defaultConfig {
    consumerProguardFiles("consumer-rules.pro")
  }
}
