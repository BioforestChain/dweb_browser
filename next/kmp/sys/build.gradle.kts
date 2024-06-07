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
      implementation(projects.core)
      implementation(projects.window)
    }
  }
  kmpAndroidTarget(project) {
    dependencies {
      // Compose 相关
      implementation(libs.compose.material3)
      implementation(libs.compose.material3.window)
      // Google.accompanist 相关
      implementation(libs.accompanist.webview)
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

//  sourceSets.androidUnitTest {
//    dependencies {
//      implementation(libs.androidx.test.core)
//      implementation(libs.androidx.compose.ui.test)
//      implementation(libs.androidx.compose.ui.test.junit4)
//      implementation(libs.androidx.compose.ui.test.manifest)
//    }
//  }
  kmpIosTarget(project)
  kmpDesktopTarget(project) {
    dependencies {
      // 不直接使用 projects.libBiometrics，因为如果 disabled 了 desktop，那么就会解析不过
      implementation(project(":lib_biometrics"))
      // zxing 解析二维码
      implementation(libs.camera.zxing.code)
      implementation(libs.camera.zxing.javase)
    }
  }
}
