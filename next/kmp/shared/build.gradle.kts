plugins {
  id("kmp-compose")
}

kotlin {
  listOf(
    iosX64(), iosArm64(), iosSimulatorArm64()
  ).forEach {
    it.binaries.framework {
      baseName = "DwebShared"
      isStatic = true
    }
  }
  kmpComposeTarget(project) {
    dependencies {
      implementation(projects.helper)
      implementation(projects.pureIO)
      implementation(projects.helperCompose)
      implementation(projects.helperPlatform)
      implementation(projects.pureHttp)
      implementation(projects.pureImage)
      implementation(projects.dWebView)
      implementation(projects.window)
      implementation(projects.browser)
      implementation(projects.core)
      implementation(projects.sys)
    }
  }
  kmpIosTarget(project)
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

