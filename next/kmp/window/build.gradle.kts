import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  id("kmp-compose")
}

kotlin {
  kmpCommonTarget(project)

  kmpComposeTarget(project) {
    dependencies {
      implementation(projects.helper)
      implementation(projects.helperCompose)
      implementation(projects.helperPlatform)
      implementation(projects.pureIO)
      implementation(projects.pureHttp)
      implementation(projects.pureImage)
      implementation(projects.core)
    }
  }
  kmpAndroidTarget(project) {
    dependencies {
      // Compose 相关
      implementation(libs.compose.material3)
      // Google.accompanist 相关
      implementation(libs.accompanist.navigation.material)
      implementation(libs.accompanist.navigation.animation)
      implementation(libs.accompanist.systemui.controller)
      implementation(libs.accompanist.permissions)
      implementation(libs.accompanist.insets.ui)
      implementation(libs.compose.ui.preview)
    }
  }
  kmpIosTarget(project)
  kmpDesktopTarget(project) {
    dependencies {
      implementation(libs.dweb.resvg.render)
    }
  }

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  applyHierarchyPlatformTemplate {
    common {
      group("mobile") {
        withAndroidTarget()
        withIosTarget()
      }
      withDesktopTarget()
    }
  }
  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  applyHierarchyPlatformTemplate {
    common {
      withAndroidTarget()
      withIosTarget()
      withDesktopTarget()
    }
  }
}