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
        implementation(project(":helperCompose"))
        implementation(project(":helperPlatform"))
        implementation(project(":microService"))
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
      }
    }
    val androidMain by getting {
      dependencies {
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.activity)
        implementation(libs.androidx.activity.ktx)
        implementation(libs.androidx.appcompat)
        implementation(libs.androidx.animation.core.android)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.activity.compose)
        // Google.accompanist 相关
        implementation(libs.accompanist.webview)
        implementation(libs.accompanist.navigation.material)
        implementation(libs.accompanist.navigation.animation)
        implementation(libs.accompanist.systemui.controller)
        implementation(libs.accompanist.permissions)
        implementation(libs.accompanist.insets.ui)

      }
    }
  }
}

android {
  namespace = "org.dweb_browser.window"
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
  sourceSets["main"].apply {
    res.srcDirs("src/androidMain/res", "src/commonMain/res", "src/main/res")
  }
}