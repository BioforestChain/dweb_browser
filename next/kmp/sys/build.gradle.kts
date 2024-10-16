import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  id("kmp-compose")
}

kotlin {
  kmpComposeTarget(project) {
    dependencies {
      implementation(projects.helper)
      implementation(projects.pureIO)
      implementation(projects.helperCompose)
      implementation(projects.helperPlatform)
      implementation(projects.pureHttp)
      implementation(projects.pureImage)
      implementation(projects.pureCrypto)
      implementation(projects.core)
      implementation(projects.window)

      implementation(libs.jetbrains.compose.material3.adaptive)
      implementation(libs.jetbrains.compose.material3.adaptive.layout)
      implementation(libs.jetbrains.compose.material3.adaptive.navigation)
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
      //
      implementation(libs.google.material)
      // 生物识别
      implementation(libs.androidx.biometric.ktx)
      // 扫码核心库
      implementation(libs.camera.barcode)
    }
  }

  kmpIosTarget(project)
  kmpDesktopTarget(project) {
    dependencies {
      // 不直接使用 projects.*，因为如果 disabled 了 desktop，那么就会解析不过
      implementation(libs.dweb.biometrics)
      implementation(libs.dweb.hardware.info)

      // 文件选择器
      api(libs.filekit.compose)
    }
  }
  sourceSets.create("nativeJvmMain") {
    dependencies {
      implementation(libs.dweb.keychainstore)
    }
  }

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  applyHierarchyPlatformTemplate {
    common {
      group("nativeJvm") {
        withIosTarget()
        withDesktopTarget()
      }
      withAndroidTarget()
    }
  }

}
