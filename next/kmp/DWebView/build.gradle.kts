import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  alias(libs.plugins.kotlinxMultiplatform)
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.kotlinPluginSerialization)
}

@OptIn(ExperimentalKotlinGradlePluginApi::class)
kotlin {
  androidTarget {
    compilations.all {
      kotlinOptions {
        jvmTarget = libs.versions.jvmTarget.get()
      }
    }
  }
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmTarget.get()))
  }

  listOf(
    iosX64(), iosArm64(), iosSimulatorArm64()
  ).forEach {
    it.binaries.framework {
      baseName = "DWebView"
    }
  }

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

  applyDefaultHierarchyTemplate()

  sourceSets.commonMain.dependencies {
    api(kotlin("stdlib"))
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.atomicfu)

    implementation(project(":helper"))
    implementation(project(":core"))
    implementation(project(":helperPlatform"))
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
}