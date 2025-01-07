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
      implementation(projects.helperCapturable)
      implementation(projects.helperPlatform)
      implementation(projects.pureIO)
      implementation(projects.pureCrypto)
      implementation(projects.pureImage)
      implementation(projects.pureHttp)
      implementation(projects.core)
      implementation(projects.sys)
      implementation(projects.dwebview)
      implementation(projects.window)

      implementation(libs.jetbrains.compose.material3.adaptive)
      implementation(libs.jetbrains.compose.material3.adaptive.layout)
      implementation(libs.jetbrains.compose.material3.adaptive.navigation)
    }
    testDependencies {
      implementation(projects.dwebview)
    }
  }
  kmpAndroidTarget(project) {
    dependencies {
      // AndroidX Compose
      implementation(libs.compose.material3)
      // Google Accompanist
      implementation(libs.accompanist.permissions)
      implementation(libs.compose.ui.preview)
      //扫码核心库
      implementation(libs.camera.core)
      implementation(libs.camera.view)
      implementation(libs.camera.camera2)
      implementation(libs.camera.lifecycle)
      implementation(libs.camera.barcode)
    }
  }
  kmpIosTarget(project) {
    dependencies {
      //解压缩
      implementation(libs.dweb.ziplib)
    }
  }
  kmpDesktopTarget(project)
  sourceSets.jvmMain {
    dependencies {
      // zxing 解析二维码
      implementation(libs.camera.zxing.code)
      implementation(libs.camera.zxing.javase)
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
      group("jvm") {
        withDesktopTarget()
        withAndroidTarget()
      }
      withIosTarget()
    }
  }
}
