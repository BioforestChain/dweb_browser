plugins {
  id("mobile-compose-target")
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

  sourceSets.commonMain.dependencies {
    implementation(libs.jetbrains.compose.runtime)
    implementation(libs.jetbrains.compose.foundation)
    implementation(libs.jetbrains.compose.components.resources)
    implementation(libs.jetbrains.compose.material3)

    implementation(projects.helper)
    implementation(projects.helperCompose)
    implementation(projects.helperPlatform)
    implementation(projects.window)
    implementation(projects.browser)
    implementation(projects.core)
    implementation(projects.sys)
  }

  sourceSets.commonTest.dependencies {
    implementation(kotlin("test"))
  }
}

android {
  namespace = "org.dweb_browser.shared"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
  }

  sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
  sourceSets["main"].res.srcDirs("src/androidMain/res")
  sourceSets["main"].resources.srcDirs("src/androidMain/resources","src/commonMain/resources")
}