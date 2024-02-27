plugins {
  id("kmp-compose")
}

kotlin {
  kmpComposeTarget(project) {
    dependencies {
      implementation(projects.helper)
      implementation(projects.helperPlatform)
      implementation(projects.helperCompose)
      implementation(projects.pureIO)
      implementation(projects.pureHttp)
      implementation(projects.pureImage)
      implementation(projects.core)
      implementation(projects.sys)
    }
  }
  kmpAndroidTarget(project) {
    dependencies {
      implementation(libs.androidx.webkit)
      implementation(libs.google.material)
      implementation(libs.accompanist.webview)
      implementation(libs.compose.ui)

      implementation(projects.libReverseProxy)
    }
//    instrumentedTestDependsOn(commonMain.get())
//    instrumentedTestDependsOn(androidMain.get())
//    instrumentedTestSourceSets{
//      add(androidMain.get())
//    }
    instrumentedTestDependencies {
      implementation(libs.androidx.test.core)
      implementation(libs.androidx.compose.ui.test)
      implementation(libs.androidx.compose.ui.test.junit4)
      implementation(libs.androidx.compose.ui.test.manifest)
    }
  }
  kmpIosTarget(project) {
    dependencies {
      implementation(projects.libReverseProxy)
    }
  }
  kmpDesktopTarget(project)
}

