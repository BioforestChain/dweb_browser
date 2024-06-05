import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import com.android.build.gradle.internal.tasks.FinalizeBundleTask
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import java.util.Properties

plugins {
  id("target-compose")
  id("target-common")
  id(libs.plugins.androidApplication.get().pluginId)
  alias(libs.plugins.androidxBaselineprofile)
}

kotlin {
  kmpComposeTarget(project) {
    dependencies {
      implementation(projects.shared) // 用于导入 resources 文件夹
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

val keystoreProperties = Properties()
val keystorePropertiesFile = project.file("key.properties")
if (keystorePropertiesFile.exists()) {
  keystoreProperties.load(keystorePropertiesFile.inputStream())
}

android {
  namespace = "info.bagen.dwebbrowser"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  val localProperties = localProperties()
  defaultConfig {
    applicationId = "info.bagen.dwebbrowser"
    minSdk = libs.versions.minSdkVersion.get().toInt()
    targetSdk = libs.versions.targetSdkVersion.get().toInt()
    versionCode = libs.versions.versionCode.get().toInt()
    versionName = libs.versions.versionName.get()


    val needarmeabiv7a = localProperties.getBoolean("android.build.ndk.armeabi-v7a")
    val needx86 = localProperties.getBoolean("android.build.ndk.x86")

    ndk.abiFilters.addAll(
      listOf("arm64-v8a").let {
        if (needarmeabiv7a) it + "armeabi-v7a" else it
      }.let {
        if (needx86) it + "x86" else it
      })
  }
  baselineProfile {
    mergeIntoMain = true
    dexLayoutOptimization = true
    saveInSrc = true
    baselineProfileOutputDir = "generated/baselineProfiles"
    // ./gradlew androidApp:generateBaselineProfile
    automaticGenerationDuringBuild = false
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

  // 获取本地配置文件
  val props = Properties().also { properties ->
    rootDir.resolve("local.properties").apply {
      if (exists()) {
        inputStream().use { properties.load(it) }
      }
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

      keyAlias = keystoreProperties["keyAlias"]?.toString()
      keyPassword = keystoreProperties["keyPassword"]?.toString()
      storeFile = keystoreProperties["storeFile"]?.let { file(it.toString()) }
      storePassword = keystoreProperties["storePassword"]?.toString()
      println("qqqq storeFile=${storeFile}")
    }
    getByName("debug") {
      // 获取本地配置的 key 信息，storeFile 是将jks文件放在当前 build.gradle.kts 同级目录
      props.getProperty("keyAlias", null)?.let { keyAlias = it }
      props.getProperty("keyPassword", null)?.let { keyPassword = it }
      props.getProperty("storeFile", null)?.let { storeFile = file(it) }
      props.getProperty("storePassword", null)?.let { storePassword = it }
    }
  }

  android.buildFeatures.buildConfig = true
  buildTypes {
    getByName("release") {
      // signingConfig = signingConfigs.getByName("debug") // 如果是测试benchmark需要使用debug
      signingConfig = signingConfigs.getByName("release")
      isMinifyEnabled = true // 开启代码混淆
      setProguardFiles(listOf("proguard-rules.pro"))
      isShrinkResources = true // 移除无用的resource
      resValue("string", "appName", "Dweb Browser")
      applicationIdSuffix = null
      versionNameSuffix = null
    }
    getByName("debug") {
      signingConfig = signingConfigs.getByName("debug")
      val userName = props.getProperty("user.name", null)
        ?: System.getProperty("user.name").replace("[^a-zA-Z0-9]".toRegex(), "").lowercase()
      resValue("string", "appName", "Kmp-$userName")
      applicationIdSuffix = ".kmp.$userName"
      versionNameSuffix = null // ".kmp.$userName"
    }
    create("benchmark") {
      initWith(buildTypes.getByName("release"))
      matchingFallbacks += listOf("release")
      isDebuggable = false
    }
  }

  flavorDimensions += listOf("abi")
  productFlavors {
//    create("debug") {
//      signingConfig = signingConfigs.getByName("debug")
//      resValue("string", "appName", "🧪DwebBrowser")
//      applicationIdSuffix = ".debug"
//    }

//    if (localProperties.getBoolean("android.build.ndk.armeabi-v7a")) {
    create("withArm32") {
      dimension = "abi"
      matchingFallbacks += listOf("release")
      ndk.abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
    }
//    }
//    if (localProperties.getBoolean("android.build.ndk.x86")) {
    create("withX86") {
      dimension = "abi"
      matchingFallbacks += listOf("release")
      ndk.abiFilters.addAll(listOf("arm64-v8a", "x86"))
    }
//    }
  }
  applicationVariants.all {
    outputs.all {
      val midName = when {
        name.startsWith("with") -> "_" + name.substring(4).split("-").first().lowercase()
        else -> ""
      }
      val archivesName = "DwebBrowser${midName}_v${libs.versions.versionName.get()}"

      if (buildType.name == "release") {
        // 修改bundle名
        val bundleFinalizeTaskName = StringBuilder("sign").run {
          append(flavorName.capitalizeAsciiOnly())
          append(buildType.name.capitalizeAsciiOnly())
          append("Bundle")
          toString()
        }
        tasks.named(bundleFinalizeTaskName, FinalizeBundleTask::class) {
          val file = finalBundleFile.asFile.get()
          finalBundleFile.set(File(file.parentFile, "$archivesName.aab"))
        }

        // 修改apk名
        if (this is ApkVariantOutputImpl) {
          outputFileName = "$archivesName.apk"
        }
      } else {
        // 修改apk名
        if (this is ApkVariantOutputImpl) {
          outputFileName = "${archivesName}_debug.apk"
        }
      }
    }
  }
}

dependencies {
  implementation(libs.androidx.profile.installer)
//  baselineProfile(projects.androidBenchmark)
  baselineProfile(projects.androidBaselineprofile)
}