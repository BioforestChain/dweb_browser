package com.version.manager

/**
 * 配置和 build相关的
 */
object BuildVersion {
  const val compileSdkVersion = 33
  const val buildToolsVersion = "29.0.2"
  const val minSdkVersion = 28
  const val targetSdkVersion = 33
  const val versionCode = 6
  const val versionName = "1.1.5"

  const val kotlinVersion = "1.8.10"
}

/**
 * 项目相关配置
 */
object BuildConfig {
  // 网络开发相关
  const val http4kBom = "org.http4k:http4k-bom:5.5.0.0"
  const val http4kCore = "org.http4k:http4k-core"
  const val http4kMultipart = "org.http4k:http4k-multipart"
  const val http4kClientApache = "org.http4k:http4k-client-apache"
  const val http4kWebSocket = "org.http4k:http4k-client-websocket"
  const val http4kServerKtorcio = "org.http4k:http4k-server-ktorcio"
  const val http4kServerNetty = "org.http4k:http4k-server-netty"

  const val ktor_version = "2.2.3"
  const val ktorServerWebsockets = "io.ktor:ktor-server-websockets:$ktor_version"
  const val ktorServerNetty = "io.ktor:ktor-server-netty:$ktor_version"
  const val ktorServerCio = "io.ktor:ktor-server-cio:$ktor_version"

  // 谷歌支持库
  private const val accompanistVersion = "0.31.3-beta"
  const val accompanistWebview = "com.google.accompanist:accompanist-webview:$accompanistVersion"
  const val accompanistNavigationMaterial =
    "com.google.accompanist:accompanist-navigation-material:$accompanistVersion"
  const val accompanistNavigationAnimation =
    "com.google.accompanist:accompanist-navigation-animation:$accompanistVersion"
  const val accompanistSystemUiController =
    "com.google.accompanist:accompanist-systemuicontroller:$accompanistVersion"
  const val accompanistPermissions =
    "com.google.accompanist:accompanist-permissions:$accompanistVersion"
  const val accompanistInsets = "com.google.accompanist:accompanist-insets:$accompanistVersion"
  const val accompanistInsetsUI = "com.google.accompanist:accompanist-insets-ui:$accompanistVersion"

  // androidX
  const val appcompat = "androidx.appcompat:appcompat:1.6.1"
  const val appcompatResources = "androidx.appcompat:appcompat-resources:1.6.1"
  const val activity = "androidx.activity:activity:1.8.0-alpha04" // Java language implementation
  const val activityKotlin = "androidx.activity:activity-ktx:1.8.0-alpha04" // Kotlin
  const val activityCompose = "androidx.activity:activity-compose:1.8.0-alpha04"
  const val coreKotlin = "androidx.core:core-ktx:1.10.1"
  const val coreSplashScreen = "androidx.core:core-splashscreen:1.1.0-alpha01"
  const val navigationUiKotlin = "androidx.navigation:navigation-ui-ktx:2.5.3"
  const val gridLayout = "androidx.gridlayout:gridlayout:1.0.0"
  const val profileInstaller = "androidx.profileinstaller:profileinstaller:1.3.1"
  const val lifecycleRuntimeKotlin = "androidx.lifecycle:lifecycle-runtime-ktx:2.6.1"

  // 键值存储
  const val dataStore = "androidx.datastore:datastore:1.0.0"
  const val dataStorePreferences = "androidx.datastore:datastore-preferences:1.1.0-alpha04"

  // room 数据库存储
  const val roomRuntime = "androidx.room:room-runtime:2.5.1"
  const val roomCompiler =
    "androidx.room:room-compiler:2.5.1" // To use Kotlin annotation processing tool (kapt)
  const val roomKotlin = "androidx.room:room-ktx:2.5.1" // kotlin扩展和协同程序对Room的支持

  // 生物识别
  const val biometric = "androidx.biometric:biometric:1.2.0-alpha05"

  // 数据处理
  const val gson = "com.google.code.gson:gson:2.10.1"
  const val moshiPack = "com.daveanthonythomas.moshipack:moshipack:1.0.1" // message-pack

  // 解压文件
  const val commonsCompress = "org.apache.commons:commons-compress:1.23.0"

  /// Compose 相关
  const val composeBom = "androidx.compose:compose-bom:2023.05.01"
  const val composeUI = "androidx.compose.ui:ui"
  const val composeMaterial = "androidx.compose.material:material"
  const val composeMaterialIcons = "androidx.compose.material:material-icons-extended"
  const val composeMaterial3 = "androidx.compose.material3:material3:1.2.0-alpha02"
  const val composeMaterial3Window =
    "androidx.compose.material3:material3-window-size-class:1.2.0-alpha02"
  const val composeUIPreview = "androidx.compose.ui:ui-tooling-preview"
  const val composeUIGraphics = "androidx.compose.ui:ui-graphics"
  const val composeAndroidTest = "androidx.compose.ui:ui-test-junit4:1.5.0-beta01"
  const val composeDebugUITooling = "androidx.compose.ui:ui-tooling"
  const val composeDebugUIManifest = "androidx.compose.ui:ui-test-manifest"
  const val googleMaterial = "com.google.android.material:material"

  //扫码核心库
  const val barcodeScanning = "com.google.mlkit:barcode-scanning:17.1.0"
  private const val cameraVersion = "1.3.0-alpha07"
  const val cameraCore = "androidx.camera:camera-core:$cameraVersion"
  const val cameraCamera2 = "androidx.camera:camera-camera2:$cameraVersion"
  const val cameraView = "androidx.camera:camera-view:$cameraVersion"
  const val cameraLifecycle = "androidx.camera:camera-lifecycle:$cameraVersion"

  // 加载图片 coil
  private const val coilVersion = "2.4.0"
  const val coil = "io.coil-kt:coil:$coilVersion"
  const val coilCompose = "io.coil-kt:coil-compose:$coilVersion"
  const val coilSVG = "io.coil-kt:coil-svg:$coilVersion"
  const val coilVideo = "io.coil-kt:coil-video:$coilVersion"
  const val coilGif = "io.coil-kt:coil-gif:$coilVersion"

  const val kotlinxCoroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1"

  //////// 测试相关
  /// 测试相关
  const val testKotlinCoroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1"
  const val testKotlinCoroutinesDebug = "org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.7.1"
  const val testJUnitBom = "org.junit:junit-bom:5.9.2"
  const val testJUnitPlatformLauncher = "org.junit.platform:junit-platform-launcher"
  const val testJUnitJupiter = "org.junit.jupiter:junit-jupiter"
  const val testJUnitJupiterEngine = "org.junit.jupiter:junit-jupiter-engine"
  const val testJUnitVintageEngine = "org.junit.vintage:junit-vintage-engine"
}