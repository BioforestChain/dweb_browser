import com.version.manager.BuildConfig
import com.version.manager.BuildVersion

plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
  id("com.google.devtools.ksp")
  id("com.version.manager")
}

android {
  namespace = "org.dweb_browser.browserUI"
  compileSdk = BuildVersion.compileSdkVersion
  defaultConfig {
    minSdk = BuildVersion.minSdkVersion

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
    sourceCompatibility = BuildVersion.javaVersion
    targetCompatibility = BuildVersion.javaVersion
  }
  kotlinOptions {
    jvmTarget = BuildVersion.jvmTarget
  }
  buildFeatures {
    compose = true
  }
  composeOptions {
    kotlinCompilerExtensionVersion = BuildVersion.kotlinCompilerExtensionVersion
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
      // 添加 http4k 框架后，会有异常报错，需要添加如下内容
      excludes += "/META-INF/INDEX.LIST"
      excludes += "/META-INF/io.netty.versions.properties"
      excludes += "/META-INF/DEPENDENCIES"
    }
  }
}

dependencies {
  implementation(BuildConfig.coreKotlin)
  implementation(BuildConfig.appcompat)
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

  implementation(BuildConfig.cameraCore)
  implementation(BuildConfig.cameraView)
  implementation(BuildConfig.cameraCamera2)
  implementation(BuildConfig.cameraLifecycle)
  implementation(BuildConfig.barcodeScanning)

  implementation(BuildConfig.accompanistPermissions)

  implementation(BuildConfig.commonsCompress)
  api(BuildConfig.dataStore)
  api(BuildConfig.dataStorePreferences)

  implementation(project(mapOf("path" to ":helper")))
  implementation(project(mapOf("path" to ":DWebView")))
  implementation(project(mapOf("path" to ":MicroService")))
}