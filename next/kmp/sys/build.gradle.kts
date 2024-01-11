plugins {
  id("kmp-compose")
}

kotlin {
  kmpComposeTarget(libs) {
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
  kmpAndroidTarget(libs) {
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
      implementation(libs.androidx.biometric)
      //扫码核心库
      implementation(libs.camera.barcode)
      // 获取定位
      implementation(libs.google.location)
    }
  }

  sourceSets.androidUnitTest {
    dependencies {
      implementation(libs.androidx.test.core)
      implementation(libs.androidx.compose.ui.test)
      implementation(libs.androidx.compose.ui.test.junit4)
      implementation(libs.androidx.compose.ui.test.manifest)
    }
  }
}

android {
  namespace = "org.dweb_browser.sys"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
  }

  packaging {
    resources {
      excludes += "/META-INF/DEPENDENCIES"
    }
  }
  sourceSets["main"].apply {
    res.srcDirs("src/androidMain/res", "src/commonMain/res", "src/main/res")
  }
//  sourceSets["androidTest"].apply {
//    res.srcDirs("src/androidMain/res", "src/commonMain/res", "src/main/res")
//  }
//  sourceSets["instrumentedTest"].apply {
//    res.srcDirs("src/androidMain/res", "src/commonMain/res", "src/main/res")
//  }
}

