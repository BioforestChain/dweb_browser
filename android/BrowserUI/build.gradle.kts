import com.version.manager.BuildConfig
import com.version.manager.BuildVersion

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("com.google.devtools.ksp")
  id("com.version.manager")
}

android {
  namespace = "org.dweb_browser.browserUI"
  compileSdk = BuildVersion.compileSdkVersion

  defaultConfig {
    applicationId = "org.dweb_browser.browserUI"
    minSdk = BuildVersion.minSdkVersion
    targetSdk = BuildVersion.targetSdkVersion
    versionCode = BuildVersion.versionCode
    versionName = BuildVersion.versionName

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables {
      useSupportLibrary = true
    }
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
    jvmTarget = "1.8"
  }
  buildFeatures {
    compose = true
  }
  composeOptions {
    kotlinCompilerExtensionVersion = "1.4.3"
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

dependencies {
  implementation(BuildConfig.coreKotlin)
  implementation(BuildConfig.lifecycleRuntimeKotlin)
  implementation(BuildConfig.activityCompose)

  implementation(BuildConfig.composeBom)
  androidTestImplementation(platform(BuildConfig.composeBom))
  implementation(BuildConfig.composeUI)
  implementation(BuildConfig.composeUIGraphics)
  implementation(BuildConfig.composeUIPreview)
  implementation(BuildConfig.composeMaterial3)
  implementation(BuildConfig.composeMaterialIcons)
  androidTestImplementation(BuildConfig.composeAndroidTest)
  debugImplementation(BuildConfig.composeDebugUITooling)
  debugImplementation(BuildConfig.composeDebugUIManifest)

  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test.ext:junit:1.1.5")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

  implementation(project(mapOf("path" to ":helper")))

  // 增加 room 存储列表数据
  implementation(BuildConfig.roomRuntime)
  ksp(BuildConfig.roomCompiler)
  implementation(BuildConfig.roomKotlin) // kotlin扩展和协同程序对Room的支持

  // 加载图片 coil
  implementation(BuildConfig.coil)
  implementation(BuildConfig.coilCompose)
  implementation(BuildConfig.coilSVG)
  implementation(BuildConfig.coilVideo)
  implementation(BuildConfig.coilGif)
}