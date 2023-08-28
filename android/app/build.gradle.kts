@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.kotlinAndroid)
  alias(libs.plugins.kotlinPluginSerialization)
}
android {
  namespace = "info.bagen.dwebbrowser"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  compileSdkPreview = "UpsideDownCake"
  defaultConfig {
    applicationId = "info.bagen.dwebbrowser"
    testApplicationId = "$applicationId.test"
    minSdk = libs.versions.minSdkVersion.get().toInt()
    targetSdk = libs.versions.targetSdkVersion.get().toInt()
    versionCode = libs.versions.versionCode.get().toInt()
    versionName = libs.versions.versionName.get()
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    //ndk.abiFilters.addAll(listOf("armeabi-v7a", "x86", "x86_64"))
    // ndk.abiFilters.addAll(listOf("arm64-v8a", "x86")) // 上架需要32位的包
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
    named("androidTest") {
      res.setSrcDirs(listOf("src/androidTest/res"))
    }
  }

  signingConfigs {
    create("release") {
      // 使用 keytool -printcert -jarfile app_release.apk 直接打印 jar 签名信息
      // 使用 jarsigner --verify app_release.apk 提示"jar 已验证。"
      // 使用 apksigner verify -v app_release.apk 显示如下，V1 为false，但实际是有签名的。只有当minSdk改为<24的版本，这边的验证才会提现为true
      //    Verified using v1 scheme (JAR signing): false
      //    Verified using v2 scheme (APK Signature Scheme v2): true
      //    Verified using v3 scheme (APK Signature Scheme v3): false
      //    Verified using v3.1 scheme (APK Signature Scheme v3.1): false
      enableV1Signing = true
      enableV2Signing = true
      enableV3Signing = false
      enableV4Signing = false
    }
  }

  buildTypes {
    release {
      signingConfig = signingConfigs.getByName("release")
      isMinifyEnabled = true //开启代码混淆
      setProguardFiles(listOf("proguard-rules.pro"))
      isShrinkResources = true //移除无用的resource文件
    }
    debug {
      defaultConfig {
        val userName = System.getProperty("user.name")
          .replace("[^a-zA-Z0-9]".toRegex(), "").lowercase()
        resValue("string", "appName", "Dev-$userName")
        applicationIdSuffix = "debug$userName"
        versionNameSuffix = "debug-$userName"
      }
    }
    create("benchmark") {
      matchingFallbacks += listOf("debug")
      isDebuggable = false
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.valueOf(libs.versions.javaVersion.get())
    targetCompatibility = JavaVersion.valueOf(libs.versions.javaVersion.get())
  }
  kotlinOptions {
    jvmTarget = libs.versions.jvmTarget.get()
    freeCompilerArgs += listOf(
      "-P",
      "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=${libs.versions.kotlin.version.get()}"
    )
  }
  buildFeatures {
    compose = true
    viewBinding = true
  }
  composeOptions {
    kotlinCompilerExtensionVersion = libs.versions.kotlinCompilerExtensionVersion.get()
  }
  lint {
    abortOnError = false
    warning += listOf("InvalidPackage")
  }
  packaging {
    resources {
      pickFirsts += "win32-x86-64/attach_hotspot_windows.dll"
      pickFirsts += "win32-x86/attach_hotspot_windows.dll"
      excludes += "/META-INF/licenses/ASM"
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
      // 添加 http4k 框架后，会有异常报错，需要添加如下内容
      excludes += "/META-INF/INDEX.LIST"
      excludes += "/META-INF/io.netty.versions.properties"
      excludes += "/META-INF/DEPENDENCIES"
    }
  }
}

dependencies {
  /// 网络开发相关
  //implementation(platform(libs.http4k.bom))
  //implementation(libs.http4k.core)
  //implementation(libs.http4k.multipart)
  //implementation(libs.http4k.client.apache)
  //implementation(libs.ktor.client.cio)

  // Android 相关
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.navigation.ui.kotlin)
  implementation(libs.androidx.gridlayout)

  // Google.accompanist 相关
  implementation(libs.accompanist.webview)
  implementation(libs.accompanist.navigation.material)
  implementation(libs.accompanist.navigation.animation)
  implementation(libs.accompanist.systemui.controller)
  implementation(libs.accompanist.permissions)
  implementation(libs.accompanist.insets.ui)

  implementation(libs.androidx.profile.installer)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.appcompat.resources)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.activity) // Java language implementation
  implementation(libs.androidx.activity.ktx) // Kotlin
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.lifecycle.service)

  /// Compose 相关
  implementation(platform(libs.compose.bom))
  androidTestImplementation(platform(libs.compose.bom))
  implementation(libs.compose.ui)
  implementation(libs.compose.material)
  implementation(libs.compose.material3)
  implementation(libs.compose.material3.window)
  implementation(libs.compose.material.icons)
  implementation(libs.compose.ui.preview)
  androidTestImplementation(libs.compose.ui.test.junit4)
  debugImplementation(libs.compose.ui.tooling)
  implementation(libs.google.material)

  //扫码核心库
  implementation(libs.camera.barcode)
  //implementation(libs.camera.core)
  //implementation(libs.camera.camera2)
  //implementation(libs.camera.view)
  //implementation(libs.camera.lifecycle)

  /// 测试相关
  testImplementation(kotlin("test"))
  testImplementation(libs.test.kotlin.coroutines.test)
  testImplementation(libs.test.kotlin.coroutines.debug)
  testImplementation(platform(libs.test.junit.bom))
  testRuntimeOnly(libs.test.junit.platform.launcher) {
    because("Only needed to run tests in a version of IntelliJ IDEA that bundles older versions")
  }
  testImplementation(libs.test.junit.jupiter)
  testRuntimeOnly(libs.test.junit.jupiter.engine)
  testRuntimeOnly(libs.test.junit.vintage.engine)

  // 加载图片 coil
  implementation(libs.coil.core)
  implementation(libs.coil.compose)
  implementation(libs.coil.svg)
  implementation(libs.coil.video)
  implementation(libs.coil.gif)
  // 生物识别
  implementation(libs.androidx.biometric)
  /// 依赖
  implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

  implementation(project(mapOf("path" to ":helper")))
  implementation(project(mapOf("path" to ":MicroService")))
  implementation(project(mapOf("path" to ":DWebView")))
  implementation(project(mapOf("path" to ":BrowserUI")))
  implementation(project(mapOf("path" to ":window")))
  implementation(project(mapOf("path" to ":helperAndroid")))
}

tasks.withType<Test> {
  useJUnitPlatform()
  jvmArgs("--enable-preview")
}