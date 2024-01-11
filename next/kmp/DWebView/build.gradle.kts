plugins {
  id("kmp-compose")
}

kotlin {
  kmpComposeTarget(libs) {
    dependencies {
      implementation(projects.helper)
      implementation(projects.helperPlatform)
      implementation(projects.helperCompose)
      implementation(projects.pureIO)
      implementation(projects.pureHttp)
      implementation(projects.pureImage)
      implementation(projects.core)
      implementation(projects.sys)
      implementation(projects.reverseProxy)
    }
  }
  kmpAndroidTarget(libs) {
    dependencies {
      implementation(libs.androidx.webkit)
      implementation(libs.google.material)
      implementation(libs.accompanist.webview)
      implementation(libs.compose.ui)
    }
  }
  sourceSets {
    androidInstrumentedTest {
      dependsOn(commonMain.get())
      dependsOn(androidMain.get())
      dependencies {
        implementation(libs.androidx.test.core)
        implementation(libs.androidx.compose.ui.test)
        implementation(libs.androidx.compose.ui.test.junit4)
        implementation(libs.androidx.compose.ui.test.manifest)
      }
      sourceSets {
        add(androidMain.get())
      }
    }
  }

}

android {
  namespace = "org.dweb_browser.dwebview"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}

