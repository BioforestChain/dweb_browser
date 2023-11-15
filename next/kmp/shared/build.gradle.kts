import org.dweb_browser.buildsrc.commonMobileTarget

plugins {
  id(libs.plugins.kotlinxMultiplatform.get().pluginId)
  id(libs.plugins.androidLibrary.get().pluginId)
  kotlin("plugin.serialization") version (libs.versions.kotlin.version)
}

kotlin {
  commonMobileTarget("DwebShared")

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

  composeOptions {
    kotlinCompilerExtensionVersion = libs.versions.kotlinCompilerExtensionVersion.get()
  }

  buildFeatures {
    compose = true
  }

  sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
  sourceSets["main"].res.srcDirs("src/androidMain/res")
  sourceSets["main"].resources.srcDirs("src/androidMain/resources","src/commonMain/resources")
}