plugins {
  id("mobile-compose-target")
}

kotlin {

//  js(IR){
//    binaries.executable()
//    nodejs {  }
//  }

//  applyHierarchyTemplate {
//    group("android") {
//      withAndroidTarget()
//    }
//    group("ios") {
//      withIos()
//    }
//    group("js") {
//      withJs()
//    }
//  }

  sourceSets.commonMain.dependencies {
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
    implementation(projects.core)
    implementation(projects.helperPlatform)
    implementation(projects.reverseProxy)
  }
  sourceSets.commonTest.dependencies {
    implementation(kotlin("test"))
    implementation(libs.test.kotlin.coroutines.test)
    implementation(libs.test.kotlin.coroutines.debug)
    implementation(libs.kotlinx.atomicfu)
  }
  sourceSets.androidMain.dependencies {
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
  }

  sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}

