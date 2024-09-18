plugins {
  id("kmp-compose")
}

kotlin {
  kmpComposeTarget(project) {
    dependencies {
      implementation(projects.helper)
      implementation(projects.pureIO)
      implementation(projects.helperCompose)
      implementation(projects.helperPlatform)
      implementation(projects.pureHttp)
      implementation(projects.pureImage)
      implementation(projects.dwebview)
      implementation(projects.window)
      implementation(projects.browser)
      implementation(projects.core)
      implementation(projects.sys)
    }
  }
  kmpIosTarget(project) {
    listOf(
      iosX64(), iosArm64(), iosSimulatorArm64()
    ).forEach {
      it.binaries.framework {
        baseName = "DwebShared"
        isStatic = true
        binaryOptions = mutableMapOf(
          "bundleId" to "org.dweb_browser.shared",
          "bundleVersion" to "1.0.0"
        )
      }
    }
  }
  kmpAndroidTarget(project)
  kmpDesktopTarget(project)
}

android {
  namespace = "org.dweb_browser.shared"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
  }

  sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
  sourceSets["main"].res.srcDirs("src/androidMain/res")
  sourceSets["main"].resources.srcDirs("src/androidMain/resources", "src/commonMain/resources")
}

/// 修复kotlin升级2.1.0-Beta1出现的执行顺序异常
tasks.named("syncComposeResourcesForIos") {
  dependsOn("checkSandboxAndWriteProtection")
}
