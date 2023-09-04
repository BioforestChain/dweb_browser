plugins {
  alias(libs.plugins.kotlinxMultiplatform)
  alias(libs.plugins.androidLibrary)
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

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(libs.jetbrains.compose.runtime)
        implementation(libs.jetbrains.compose.foundation)
        @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
        implementation(libs.jetbrains.compose.components.resources)

        implementation(libs.jetbrains.compose.material3)
        implementation(project(":helper"))
        implementation(project(":helperCompose"))
        implementation(project(":window"))
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
      }
    }
    val androidMain by getting
    val androidUnitTest by getting
  }
}

android {
  namespace = "org.dweb_browser.shared"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
  }
  buildFeatures {
    compose = true
  }
  composeOptions {
    kotlinCompilerExtensionVersion = libs.versions.kotlinCompilerExtensionVersion.get()
  }
}