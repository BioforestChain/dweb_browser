plugins {
  alias(libs.plugins.kotlinxMultiplatform)
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.jetbrainsCompose)
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
    val androidMain by getting {
      dependencies {
        dependencies {
          implementation(project(":shared"))
          implementation(project(":helper"))
          implementation(project(":helperCompose"))
          implementation(project(":helperPlatform"))
          implementation(project(":microService"))
          implementation(project(":window"))
          implementation(libs.androidx.appcompat)
          implementation(libs.compose.bom)
          implementation(libs.compose.ui)
          implementation(libs.compose.ui.tooling)
          implementation(libs.compose.ui.preview)
          implementation(libs.compose.ui.foundation)
          implementation(libs.compose.material)
          implementation(libs.compose.material3)
          implementation(libs.androidx.activity.compose)
        }
      }
    }
  }
}
android {
  namespace = "org.dweb_browser.app.android"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    applicationId = "org.dweb_browser.app"
    minSdk = libs.versions.minSdkVersion.get().toInt()
    targetSdk = libs.versions.targetSdkVersion.get().toInt()
    versionCode = libs.versions.versionCode.get().toInt()
    versionName = libs.versions.versionName.get()
  }
  packagingOptions {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
    }
  }
}
