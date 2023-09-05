plugins {
  alias(libs.plugins.kotlinxMultiplatform)
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.kotlinPluginSerialization)
}

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

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(kotlin("stdlib"))
        implementation(libs.compose.ui)
        implementation(libs.compose.material)
        implementation(libs.compose.material3)
        implementation(libs.compose.material3.window)
        implementation(libs.compose.material.icons)
        implementation(libs.compose.ui.preview)

        implementation(project(":helper"))
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
      }
    }
    val androidMain by getting {
      dependencies {
        // Android Runtime
        api(libs.androidx.core.ktx)
        api(libs.androidx.activity)
        api(libs.androidx.activity.ktx)
        api(libs.androidx.activity.compose)
        api(libs.androidx.appcompat)
        api(libs.androidx.animation.core.android)
        api(libs.androidx.lifecycle.runtime.ktx)
        api(libs.google.material)
        // 加载图片 coil
        api(libs.coil.core)
        api(libs.coil.compose)
        api(libs.coil.svg)
        api(libs.coil.video)
        api(libs.coil.gif)
      }
    }
  }
}

android {
  namespace = "org.dweb_browser.helper.android"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
  }

  buildFeatures {
    compose = true
  }
  composeOptions {
    kotlinCompilerExtensionVersion = libs.versions.kotlinCompilerExtensionVersion.get()
  }
}