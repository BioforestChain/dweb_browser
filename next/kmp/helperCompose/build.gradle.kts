import org.dweb_browser.buildsrc.commonMobileTarget

plugins {
  id(libs.plugins.kotlinxMultiplatform.get().pluginId)
  id(libs.plugins.androidLibrary.get().pluginId)
  kotlin("plugin.serialization") version (libs.versions.kotlin.version)
}

kotlin {
  commonMobileTarget()

  sourceSets.commonMain.dependencies {
    implementation(libs.jetbrains.compose.runtime)
    implementation(libs.jetbrains.compose.foundation)
    implementation(libs.jetbrains.compose.components.resources)

    implementation(libs.jetbrains.compose.material)
    implementation(libs.jetbrains.compose.material3)
    implementation(libs.jetbrains.compose.materialIcons)

    implementation(projects.helper)
    implementation(projects.helperPlatform)
  }
  sourceSets.commonTest.dependencies {
    implementation(kotlin("test"))
  }
}

android {
  namespace = "org.dweb_browser.helper.compose"
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
}