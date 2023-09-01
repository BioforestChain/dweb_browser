plugins {
  id("com.android.application")
  kotlin("android")
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
  buildFeatures {
    compose = true
  }
  composeOptions {
    kotlinCompilerExtensionVersion = libs.versions.kotlinCompilerExtensionVersion.get()
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
  compileOptions {
    sourceCompatibility = JavaVersion.valueOf(libs.versions.javaVersion.get())
    targetCompatibility = JavaVersion.valueOf(libs.versions.javaVersion.get())
  }
  kotlinOptions {
    jvmTarget = libs.versions.jvmTarget.get()
  }
}

dependencies {
  implementation(project(":shared"))
  implementation(libs.compose.bom)
  implementation(libs.compose.ui)
  implementation(libs.compose.ui.tooling)
  implementation(libs.compose.ui.preview)
  implementation(libs.compose.ui.foundation)
  implementation(libs.compose.material)
  implementation(libs.compose.material3)
  implementation(libs.androidx.activity.compose)
}