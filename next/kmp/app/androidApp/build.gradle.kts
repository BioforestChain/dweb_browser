import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName

plugins {
  alias(libs.plugins.kotlinxMultiplatform)
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.jetbrainsCompose)
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
    val androidMain by getting {
      dependencies {
        dependencies {
          // AndroidX
          implementation(libs.androidx.activity.compose)
          implementation(libs.androidx.appcompat)
          implementation(libs.androidx.core.ktx)
          implementation(libs.androidx.core.splashscreen)

          // Jetbrains Compose
          implementation(libs.jetbrains.compose.runtime)
          implementation(libs.jetbrains.compose.foundation)
          @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
          implementation(libs.jetbrains.compose.components.resources)

          implementation(libs.jetbrains.compose.material)
          implementation(libs.jetbrains.compose.material3)
          implementation(libs.jetbrains.compose.materialIcons)

          // Google.accompanist 相关
          implementation(libs.accompanist.webview)
          implementation(libs.accompanist.permissions)
          implementation(libs.accompanist.systemui.controller)



//          //扫码核心库
//          implementation(libs.camera.core)
//          implementation(libs.camera.view)
//          implementation(libs.camera.camera2)
//          implementation(libs.camera.lifecycle)
//          implementation(libs.camera.barcode)

          // test
          implementation(libs.compose.ui.preview)

          implementation(projects.shared)
          implementation(projects.helper)
          implementation(projects.helperCompose)
          implementation(projects.helperPlatform)
          implementation(projects.core)
          implementation(projects.window)
          implementation(projects.sys)
          implementation(projects.browser)
          implementation(projects.dWebView)
        }
      }
    }
  }
}
android {
  namespace = "info.bagen.dwebbrowser"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    applicationId = "info.bagen.dwebbrowser"
    minSdk = libs.versions.minSdkVersion.get().toInt()
    targetSdk = libs.versions.targetSdkVersion.get().toInt()
    versionCode = libs.versions.versionCode.get().toInt()
    versionName = libs.versions.versionName.get()
  }
  packaging {
    resources {
      excludes += "/META-INF/versions/9/previous-compilation-data.bin"
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
      // 添加 http4k 框架后，会有异常报错，需要添加如下内容
      excludes += "/META-INF/INDEX.LIST"
      excludes += "/META-INF/DEPENDENCIES"
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
    getByName("release") {
      signingConfig = signingConfigs.getByName("release")
      isMinifyEnabled = true // 开启代码混淆
      setProguardFiles(listOf("proguard-rules.pro"))
      isShrinkResources = true // 移除无用的resource
      resValue("string", "appName", "Dweb Browser")
      applicationIdSuffix = null
      versionNameSuffix = null
      archivesName = "Dweb Browser_v${libs.versions.versionName.get()}"
    }
    debug {
      signingConfig = signingConfigs.getByName("debug")
      val userName = System.getProperty("user.name")
        .replace("[^a-zA-Z0-9]".toRegex(), "").lowercase()
      resValue("string", "appName", "Kmp-$userName")
      applicationIdSuffix = ".kmp.$userName"
      versionNameSuffix = ".kmp.$userName"
    }
  }
}
