import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  id("kmp-compose")
}

kotlin {
  kmpCommonTarget(project)
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
      api(libs.androidx.webkit)
      implementation(libs.google.material)
      implementation(libs.compose.ui)
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
  kmpIosTarget(project)
  kmpDesktopTarget(project)
}

