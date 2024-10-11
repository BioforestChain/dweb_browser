import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  id("kmp-library")
}

kotlin {
  kmpCommonTarget(project) {
    explicitApi()
    dependencies {
      api(libs.kotlinx.datetime)
      api(libs.ktor.http)
      api(libs.ktor.io)
      implementation(libs.squareup.okio)
      api(projects.libMixCompression)
    }
  }

  sourceSets.jvmMain {
    dependencies {
      //解压缩
      implementation(libs.apache.common.compress)
//      api(libs.org.slf4j.log4j12)
    }
  }

  kmpAndroidTarget(project) {
  }
  kmpIosTarget(project) {
    dependencies {
      implementation(libs.squareup.okio)
    }
  }
  kmpDesktopTarget(project) {
  }

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  applyHierarchyPlatformTemplate {
    common {
      group("jvm") {
        withDesktopTarget()
        withAndroidTarget()
      }
      withIosTarget()
    }
  }
}
