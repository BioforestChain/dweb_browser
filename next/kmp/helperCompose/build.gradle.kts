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
    }
  }
  kmpAndroidTarget(project) {
    dependencies {
      implementation(libs.compose.material3)
    }
  }
  kmpIosTarget(project)
  kmpDesktopTarget(project)

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
      group("skiko") {
        withDesktopTarget()
        withIosTarget()
      }
      withAndroidTarget()
    }
  }
}
