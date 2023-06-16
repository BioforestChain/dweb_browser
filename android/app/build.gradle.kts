import com.version.manager.BuildConfig
import com.version.manager.BuildVersion

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("kotlin-kapt")
  id("com.version.manager") // 添加版本控制的包名
}
android {
  compileSdk = BuildVersion.compileSdkVersion
  compileSdkPreview = "UpsideDownCake"
  defaultConfig {
    applicationId = "info.bagen.dwebbrowser"
    minSdk = BuildVersion.minSdkVersion
    targetSdk = BuildVersion.targetSdkVersion
    versionCode = BuildVersion.versionCode
    versionName = BuildVersion.versionName
//  project = "Scanner"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    //ndk.abiFilters.addAll(listOf("armeabi-v7a", "x86", "x86_64"))
    ndk.abiFilters.add("arm64-v8a")
    vectorDrawables {
      useSupportLibrary = true
    }
  }

  sourceSets {
    named("main") {
      jniLibs.setSrcDirs(listOf("src/main/libs"))
      assets.setSrcDirs(listOf("src/main/assets"))
      // Add generated code folder to app module source set
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true //开启代码混淆
      setProguardFiles(
        listOf(
          getDefaultProguardFile("proguard-android-optimize.txt"),
          "proguard-rules.pro"
        )
      )
      isShrinkResources = true //移除无用的resource文件
    }
    create("benchmark") {
      matchingFallbacks += listOf("debug")
      isDebuggable = false
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions {
    jvmTarget = "17"
    freeCompilerArgs += listOf(
      "-P",
      "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=${BuildVersion.kotlinVersion}"
    )
  }
  buildFeatures {
    compose = true
    viewBinding = true
  }
  composeOptions {
    kotlinCompilerExtensionVersion = "1.4.3"
  }
  lint {
    abortOnError = false
    warning += listOf("InvalidPackage")
  }
  packagingOptions {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
      // 添加 http4k 框架后，会有异常报错，需要添加如下内容
      excludes += "/META-INF/INDEX.LIST"
      excludes += "/META-INF/io.netty.versions.properties"
      excludes += "/META-INF/DEPENDENCIES"
    }
  }
  namespace = "info.bagen.dwebbrowser"
}


dependencies {
  /// 网络开发相关
  implementation(platform(BuildConfig.http4kBom))
  implementation(BuildConfig.http4kCore)
  implementation(BuildConfig.http4kMultipart)
  implementation(BuildConfig.http4kClientApache)
  implementation(BuildConfig.http4kServerKtorcio)

  // Android 相关
  implementation(BuildConfig.coreSplashScreen)
  implementation(BuildConfig.navigationUiKotlin)
  implementation(BuildConfig.gridLayout)

  implementation(BuildConfig.accompanistWebview)
  implementation(BuildConfig.accompanistNavigationMaterial)
  implementation(BuildConfig.accompanistNavigationAnimation)
  implementation(BuildConfig.accompanistSystemUiController)
  implementation(BuildConfig.accompanistPermissions)
  implementation(BuildConfig.accompanistInsets)
  implementation(BuildConfig.accompanistInsetsUI)

  implementation(BuildConfig.profileInstaller)
  implementation(BuildConfig.appcompat)
  implementation(BuildConfig.appcompatResources)

  implementation(BuildConfig.coreKotlin)
  implementation(BuildConfig.dataStore)
  implementation(BuildConfig.dataStorePreferences)

  implementation(BuildConfig.activity) // Java language implementation
  implementation(BuildConfig.activityKotlin) // Kotlin
  implementation(BuildConfig.activityCompose)

  /// Compose 相关
  implementation(BuildConfig.composeBom)
  androidTestImplementation(BuildConfig.composeBom)
  implementation(BuildConfig.composeUI)
  implementation(BuildConfig.composeMaterial)
  implementation(BuildConfig.composeMaterial3)
  implementation(BuildConfig.composeMaterial3Window)
  implementation(BuildConfig.composeMaterialIcons)
  implementation(BuildConfig.composeUIPreview)
  androidTestImplementation(BuildConfig.composeAndroidTest)
  debugImplementation(BuildConfig.composeDebugUITooling)
  implementation(BuildConfig.googleMaterial)

  // 工具库
  implementation(kotlin("stdlib"))
  implementation(BuildConfig.gson) // gson
  implementation(BuildConfig.moshiPack) // message-pack

  //扫码核心库
  implementation(BuildConfig.barcodeScanning)
  implementation(BuildConfig.cameraCore)
  implementation(BuildConfig.cameraCamera2)
  implementation(BuildConfig.cameraView)
  implementation(BuildConfig.cameraLifecycle)

  /// 测试相关
  testImplementation(kotlin("test"))
  testImplementation(BuildConfig.testKotlinCoroutinesTest)
  testImplementation(BuildConfig.testKotlinCoroutinesDebug)
  testImplementation(platform(BuildConfig.testJUnitBom))
  testRuntimeOnly(BuildConfig.testJUnitPlatformLauncher) {
    because("Only needed to run tests in a version of IntelliJ IDEA that bundles older versions")
  }
  testImplementation(BuildConfig.testJUnitJupiter)
  testRuntimeOnly(BuildConfig.testJUnitJupiterEngine)
  testRuntimeOnly(BuildConfig.testJUnitVintageEngine)

  // 解压文件
  implementation(BuildConfig.commonsCompress)

  // 加载图片 coil
  implementation(BuildConfig.coil)
  implementation(BuildConfig.coilCompose)
  implementation(BuildConfig.coilSVG)
  implementation(BuildConfig.coilVideo)
  implementation(BuildConfig.coilGif)

  // 生物识别
  implementation(BuildConfig.biometric)

  /// 依赖
  implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

  // 增加 room 存储列表数据
  implementation(BuildConfig.roomRuntime)
  kapt(BuildConfig.roomCompiler) // To use Kotlin annotation processing tool (kapt)
  implementation(BuildConfig.roomKotlin) // kotlin扩展和协同程序对Room的支持

  implementation(project(mapOf("path" to ":helper")))
  implementation(project(mapOf("path" to ":MicroService")))
  implementation(project(mapOf("path" to ":DWebView")))
}

tasks.withType<Test> {
  useJUnitPlatform()
}