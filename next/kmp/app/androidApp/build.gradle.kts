import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName

plugins {
  id("target-compose")
  id("target-common")
  id(libs.plugins.androidApplication.get().pluginId)
}

kotlin {
  kmpComposeTarget(project) {
    dependencies {
      implementation(projects.helper)
      implementation(projects.helperCompose)
      implementation(projects.helperPlatform)
      implementation(projects.pureIO)
      implementation(projects.pureHttp)
      implementation(projects.pureImage)
      implementation(projects.core)
      implementation(projects.window)
      implementation(projects.sys)
      implementation(projects.browser)
      implementation(projects.dwebview)
    }
  }
  kmpAndroidTarget(project) {
    dependencies {
      implementation(libs.androidx.core.splashscreen)
      // Google.accompanist 相关
      implementation(libs.accompanist.webview)
      implementation(libs.accompanist.permissions)
      implementation(libs.accompanist.systemui.controller)

      implementation(libs.compose.ui.preview)
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

    ndk.abiFilters.addAll(listOf("arm64-v8a","armeabi-v7a"))
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

  android.buildFeatures.buildConfig = true
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
