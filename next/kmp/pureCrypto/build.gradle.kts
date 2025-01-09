import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  id("kmp-library")
}

kotlin {
  kmpCommonTarget(project) {
    dependencies {
      api(libs.whyoleg.cryptography.core)
    }
  }

  kmpAndroidTarget(project) {
    dependencies {
      api(libs.whyoleg.cryptography.provider.jdk)
    }
  }
  kmpIosTarget(project) {
    dependencies {
      api(libs.whyoleg.cryptography.provider.openssl3.prebuilt)
    }
  }
  kmpDesktopTarget(project) {
    dependencies {
      api(libs.whyoleg.cryptography.provider.jdk)
    }
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


