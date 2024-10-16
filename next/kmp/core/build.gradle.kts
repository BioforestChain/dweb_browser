import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  id("kmp-compose")
}

kotlin {
  kmpCommonTarget(project)
  val mobileMain by sourceSets.creating {
    dependencies {
      // ssl代理。因为桌面端直接支持 ssl 服务，所以不需要这一层 rust 提供ssl代理转发
      implementation(libs.dweb.reverse.proxy)
    }
  }
  kmpComposeTarget(project) {
    dependencies {
      implementation(projects.helper)
      implementation(projects.helperCompose)
      implementation(projects.helperPlatform)
      implementation(projects.pureIO)
      implementation(projects.pureCrypto)
      implementation(projects.pureHttp)
      implementation(libs.dweb.multipart)
    }
  }
  kmpAndroidTarget(project)
  kmpIosTarget(project)
  kmpDesktopTarget(project)

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  applyHierarchyPlatformTemplate {
    common {
      group("jvm") {
        withAndroidTarget()
        withDesktopTarget()
      }
      group("mobile") {
        withAndroidTarget()
        withIosTarget()
      }
      withIosTarget()
    }
  }
}