plugins {
  id("kmp-compose")
}

kotlin {
  kmpComposeTarget(project) {
    dependencies {
      implementation(projects.helper)
      implementation(projects.pureIO)
    }
  }
  kmpAndroidTarget(project) {
    dependencies {
      implementation(libs.accompanist.systemui.controller)
      implementation(libs.google.material)
      // 加载图片 coil
      implementation(libs.coil.core)
      implementation(libs.coil.compose)
      implementation(libs.coil.svg)
      implementation(libs.coil.video)
      implementation(libs.coil.gif)
    }
  }
  kmpIosTarget(project) {
    dependencies {
      implementation(projects.helperPlatformIos)
    }
  }
  kmpNodeJsTarget(project) {
    dependencies {
    }
  }
}

android {
  namespace = "org.dweb_browser.helper.platform"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
  }

  sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}

