import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  id("kmp-compose")
}

kotlin {
  kmpCommonTarget(project)
  kmpComposeTarget(project) {
    dependencies {
      implementation(projects.helper)
      implementation(projects.helperPlatform)
      implementation(projects.helperCompose)
      implementation(projects.pureIO)
      implementation(projects.pureHttp)

      // 加载图片 coil
      api(libs.coil.core) // 为了给其他模块直接应用 MimeType 获取的方法
      api(libs.coil.compose)
      implementation(libs.coil.network.ktor)
      implementation(libs.coil.svg)

      // ext

      implementation(libs.ktor.io)
      implementation(libs.ktor.server.websockets)
      implementation(libs.ktor.server.cio)

      implementation(libs.coil.gif)
      implementation(libs.coil.video)
    }
  }

  kmpAndroidTarget(project) {
  }
  kmpIosTarget(project) {
  }
  kmpDesktopTarget(project) {
  }

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  applyHierarchyTemplate {
    common {
      group("skiko") {
        withIosTarget()
        withDesktopTarget()
      }
      withAndroidTarget()
    }
  }
}

