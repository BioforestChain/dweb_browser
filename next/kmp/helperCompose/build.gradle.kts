plugins {
  alias(libs.plugins.kotlinxMultiplatform)
  alias(libs.plugins.androidLibrary)
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

  listOf(
    iosX64(),
    iosArm64(),
    iosSimulatorArm64()
  ).forEach {
    it.binaries.framework {
      baseName = "DwebHelperCompose"
      isStatic = true
    }
  }

  applyDefaultHierarchyTemplate()

  sourceSets.commonMain.dependencies {
    implementation(libs.jetbrains.compose.runtime)
    implementation(libs.jetbrains.compose.foundation)
    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
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
}