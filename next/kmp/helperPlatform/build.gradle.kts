plugins {
  alias(libs.plugins.kotlinxMultiplatform)
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.jetbrainsCompose)
  alias(libs.plugins.kotlinPluginSerialization)
  id("com.google.osdetector") version "1.7.3"
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

  val defPath = project.file("$rootDir/helperPlatform/src/nativeInterop/cinterop")
  val frameworkPath = project.file("$rootDir/helperPlatform/src/libs/dweb_platform_kit_ios.xcframework")

  listOf(
    iosX64(),
    iosArm64(),
    iosSimulatorArm64()
  ).forEach {
    it.binaries.framework {
      baseName = "DwebHelperCompose"
      isStatic = true
    }
    it.compilations.getByName("main") {
      val dwebPlatformKitIOS by cinterops.creating {
        defFile("$defPath/dweb_platform_kit_ios.def")

        if(it.targetName == "iosArm64") {
          compilerOpts("-framework", "DwebIOS", "-F$frameworkPath/ios-arm64/")
        } else {
          compilerOpts("-framework", "DwebIOS", "-F$frameworkPath/ios-arm64_x86_64-simulator/")
        }
        extraOpts += listOf("-compiler-option", "-fmodules")
      }
    }
    it.binaries.all {
      if(it.targetName == "iosArm64") {
        linkerOpts("-framework", "DwebIOS", "-F$frameworkPath/ios-arm64/")
      } else {
        linkerOpts("-framework", "DwebIOS", "-F$frameworkPath/ios-arm64_x86_64-simulator/")
      }
    }
  }

  applyDefaultHierarchyTemplate()

  sourceSets.commonMain.dependencies {
    api(libs.jetbrains.compose.runtime)
    api(libs.jetbrains.compose.foundation)
    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
    api(libs.jetbrains.compose.components.resources)
    api(libs.kotlinx.atomicfu)
    api(libs.ktor.server.cio)
    api(libs.ktor.client.cio)
    api(libs.ktor.client.encoding)
    api(libs.ktor.server.websockets)
    api(libs.webrtc.kmp)

    implementation(libs.jetbrains.compose.material3)

    implementation(projects.helper)
  }
  sourceSets.commonTest.dependencies {
    implementation(kotlin("test"))
  }
  sourceSets.androidMain.dependencies {
    // Android Runtime
    api(libs.androidx.core.ktx)
    api(libs.androidx.activity)
    api(libs.androidx.activity.ktx)
    api(libs.androidx.activity.compose)
    api(libs.androidx.appcompat)
    api(libs.androidx.animation.core.android)
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.accompanist.systemui.controller)
    api(libs.google.material)
    // 加载图片 coil
    api(libs.coil.core)
    api(libs.coil.compose)
    api(libs.coil.svg)
    api(libs.coil.video)
    api(libs.coil.gif)
  }
  sourceSets.iosMain.dependencies {
    api(libs.ktor.client.darwin)
  }
}

android {
  namespace = "org.dweb_browser.helper.platform"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
  }
  sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}