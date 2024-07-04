import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  id("kmp-compose")
}
dependencies {
  implementation(project(":pureCrypto"))
  implementation(project(":pureCrypto"))
  implementation(project(":pureCrypto"))
}

kotlin {
  kmpCommonTarget(project) {
    dependencies {
      implementation(projects.pureCrypto)
    }
  }
  kmpComposeTarget(project) {
    dependencies {
      implementation(projects.helper)
      /**
       * 注意，helperPlatform 不可以依赖 helperCompose
       * 因为 helperCompose是面向UI的，helperPlatform是面向Application，因此更加底层
       *
       * 但是helperPlatform中包含 helper.compose 这样的一个 namespace 文件夹，
       * 所以如果有需要，可以将 helperCompose 中的代码迁移到 helperPlatform 中来
       */
    }
  }
  kmpAndroidTarget(project) {
    dependencies {
      implementation(libs.accompanist.systemui.controller)
      implementation(libs.google.material)
    }
  }
  kmpIosTarget(project) {
  }
  kmpDesktopTarget(project) {
  }

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  applyHierarchyPlatformTemplate {
    common {
      group("skiko") {
        withIosTarget()
        withDesktopTarget()
      }
      withAndroidTarget()
    }
  }
}
