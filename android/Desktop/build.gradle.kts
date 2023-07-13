import com.version.manager.BuildConfig
import com.version.manager.BuildVersion

plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
  id("com.version.manager")
}

android {
  namespace = "org.dweb_browser.desktop"
  compileSdk = BuildVersion.compileSdkVersion

  defaultConfig {
    minSdk = BuildVersion.minSdkVersion

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions {
    jvmTarget = "17"
  }
}

dependencies {
  implementation(BuildConfig.coreKotlin)
  implementation(BuildConfig.appcompat)
  implementation(BuildConfig.googleMaterial)
  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test.ext:junit:1.1.5")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

  implementation(project(mapOf("path" to ":MicroService")))
  implementation(project(mapOf("path" to ":helper")))
  implementation(project(mapOf("path" to ":BrowserUI")))
}