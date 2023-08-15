import info.bagen.version.BuildVersion

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.kotlinAndroid)
}

android {
  namespace = "org.dweb_browser.dwebview"
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
    sourceCompatibility = BuildVersion.javaVersion
    targetCompatibility = BuildVersion.javaVersion
  }
  kotlinOptions {
    jvmTarget = BuildVersion.jvmTarget
  }
}

dependencies {

  implementation(libs.core.ktx)
  implementation(libs.androidx.activity)
  implementation(libs.androidx.activity.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.google.material)
  testImplementation(libs.test.junit)
  androidTestImplementation(libs.android.test.ext)
  androidTestImplementation(libs.android.test.espresso)

  api(libs.accompanist.webview)

  implementation(platform(libs.compose.bom))
  implementation(libs.compose.ui)

  implementation(project(mapOf("path" to ":MicroService")))
  implementation(project(mapOf("path" to ":helper")))
}