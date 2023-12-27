plugins {
  id("kmp-compose-target")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(kotlin("stdlib"))
        api(libs.kotlinx.coroutines.core)
        api(libs.kotlinx.atomicfu)

        implementation(libs.jetbrains.compose.runtime)
        implementation(libs.jetbrains.compose.foundation)
        implementation(libs.jetbrains.compose.components.resources)

        implementation(libs.jetbrains.compose.material)
        implementation(libs.jetbrains.compose.material3)
        implementation(libs.jetbrains.compose.materialIcons)

        implementation(projects.helper)
        implementation(projects.helperPlatform)
        implementation(projects.helperCompose)
        implementation(projects.pureHttp)
        implementation(projects.pureImage)
        implementation(projects.core)
        implementation(projects.reverseProxy)
      }
    }
    commonTest {
      dependencies {
        implementation(kotlin("test"))
        implementation(libs.test.kotlin.coroutines.test)
        implementation(libs.test.kotlin.coroutines.debug)
        implementation(libs.kotlinx.atomicfu)
      }
    }
    androidMain {
      dependencies {
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.activity)
        implementation(libs.androidx.activity.ktx)
        implementation(libs.androidx.appcompat)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.webkit)
        implementation(libs.google.material)

        api(libs.accompanist.webview)
        implementation(libs.compose.ui)
      }
    }
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

//  sourceSets.jsMain.dependencies {
//    implementation(kotlin("stdlib-js"))
//    implementation(npm("electron", "27.0.1"))
//  }
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

