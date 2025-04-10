import com.android.build.api.dsl.ApplicationBuildType
import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import com.android.build.gradle.internal.tasks.FinalizeBundleTask
import org.gradle.internal.extensions.stdlib.capitalized
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
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
      implementation(libs.accompanist.permissions)

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
  defaultConfig {
    applicationId = "info.bagen.dwebbrowser"
    minSdk = libs.versions.minSdkVersion.get().toInt()
    targetSdk = libs.versions.targetSdkVersion.get().toInt()
    versionCode = libs.versions.versionCode.get().toInt()
    versionName = libs.versions.versionName.get()

    ndk.abiFilters.addAll(listOf("arm64-v8a"))
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
    }
    getByName("debug") {
      // 获取本地配置的 key 信息，storeFile 是将jks文件放在当前 build.gradle.kts 同级目录
      keystoreProperties.getProperty("keyAlias", null)?.let { keyAlias = it }
      keystoreProperties.getProperty("keyPassword", null)?.let { keyPassword = it }
      keystoreProperties.getProperty("storeFile", null)?.let { storeFile = file(it) }
      keystoreProperties.getProperty("storePassword", null)?.let { storePassword = it }
    }
  }

  android.buildFeatures.buildConfig = true

  // 判断编译的时候是否传入了 -PreleaseBuild=true，表示是脚本执行
  val isReleaseBuild = hasProperty("releaseBuild") && property("releaseBuild") == "true"
  val buildChannel = when {
    hasProperty("buildChannel") -> property("buildChannel")!!.toString()
    else -> "stable"
  }

  fun ApplicationBuildType.configChannel(channel: String, buildType: ApplicationBuildType) {
    if (channel == "stable") {
      applicationIdSuffix = null
      versionNameSuffix = null
      manifestPlaceholders["appName"] = "@string/app_name";//"@string/app_name"
    } else {
      applicationIdSuffix = ".$channel"
      versionNameSuffix = "-$channel"
      manifestPlaceholders["appName"] = "Dweb Browser ${channel.uppercaseFirstChar()}"
    }

    if (buildType.name == "debug") {
      val userName =
        keystoreProperties.getProperty("debugApk", null) ?: System.getProperty("user.name")
          .replace("[^a-zA-Z0-9]".toRegex(), "").lowercase()
      applicationIdSuffix = ".dweb.$userName"
      versionNameSuffix = null
      manifestPlaceholders["appName"] = "Dweb-$userName"
    }

    manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher_${channel}"
  }

  buildTypes {
    getByName("release") {
      // signingConfig = signingConfigs.getByName("debug") // 如果是测试benchmark需要使用debug
      signingConfig = signingConfigs.getByName("release")
      isMinifyEnabled = true // 开启代码混淆
      setProguardFiles(listOf("proguard-rules.pro"))
    //  setProguardFiles(emptyList<String>())
      isShrinkResources = true // 移除无用的resource
      configChannel(buildChannel, this)
      manifestPlaceholders["isTestOnly"] = "false"
    }
    getByName("debug") {
      if (isReleaseBuild) {
        /// 尽量与 release 保持一致，只是开启了 isDebuggable
        signingConfig = signingConfigs.getByName("release")
        isMinifyEnabled = true // 开启代码混淆
        setProguardFiles(listOf("proguard-rules.pro"))
        isShrinkResources = true // 移除无用的resource
        configChannel(buildChannel, this)
        manifestPlaceholders["isTestOnly"] = "false"
      } else {
        signingConfig = signingConfigs.getByName("debug")
        configChannel(buildChannel, this)
        manifestPlaceholders["isTestOnly"] = "true"
      }
      manifestPlaceholders["appIcon"] = "@mipmap/ic_launcher_debug"
      isDebuggable = true
    }
    create("benchmark") {
      initWith(buildTypes.getByName("release"))
      matchingFallbacks += listOf("release")
      isDebuggable = false
    }
  }

  flavorDimensions += listOf("abi")
  productFlavors {
    create("forArm64") {
      dimension = "abi"
      matchingFallbacks += listOf("release")
      ndk.abiFilters.run {
        clear()
        add("arm64-v8a")
      }
    }
    create("forAll") {
      dimension = "abi"
      matchingFallbacks += listOf("release")
      ndk.abiFilters.run {
        clear()
        addAll(listOf("armeabi-v7a", "x86", "x86_64"))
      }
    }
  }
  androidComponents {
    beforeVariants { variantBuilder ->
      // 只有 release 模式只需要输出 arm64 以外的变体
      if (variantBuilder.buildType != "release" && variantBuilder.buildType != "beta") {
        if (!variantBuilder.productFlavors.contains("abi" to "forArm64")) {
          variantBuilder.enable = false
        }
      }
      if (variantBuilder.buildType != "debug" && variantBuilder.buildType != "release" && variantBuilder.buildType != "beta") {
        variantBuilder.enable = true
      }
      /// bundleRelease 模式下，只打 forAll 的包
      gradle.taskGraph.whenReady {
        if (this.hasTask(":bundleRelease")) {
          // 仅在 bundleRelease 时禁用 forArm32 的 release 变体
          if (!variantBuilder.productFlavors.contains("abi" to "forAll")) {
            variantBuilder.enable = false
          }
        }
      }
      // println("QAQ beforeVariantsBuilder=${variantBuilder.name} buildType=${variantBuilder.buildType} enable=${if (variantBuilder.enable) "✅ " else "❌ "}")
    }
  }
  applicationVariants.all {
    outputs.all {
      // println("QAQ variantBuilder=${name}")
      val midName = when {
        name.startsWith("for") -> name.substring("for".length).split("-").first().lowercase()
        else -> ""
      }
      val archivesName =
        "DwebBrowser${if (midName.isEmpty()) midName else "_$midName"}_v${libs.versions.versionName.get()}"

      if (buildType.name == "release") {
        // 修改bundle名
        val bundleFinalizeTaskName = StringBuilder("sign").run {
          append(flavorName.capitalized())
          append(buildType.name.capitalized())
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