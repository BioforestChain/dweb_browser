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
      implementation(projects.pureCrypto)
      implementation(projects.pureHttp)
      implementation(projects.libMultipart)
    }
  }
  kmpAndroidTarget(project)
  kmpIosTarget(project)
  kmpDesktopTarget(project)

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  applyDefaultHierarchyTemplate {
    common {
      group("jvm") {
        withAndroidTarget()
        withDesktopTarget()
      }
    }
  }
}