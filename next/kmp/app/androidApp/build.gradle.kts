plugins {
  alias(libs.plugins.kotlinxMultiplatform)
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.jetbrainsCompose)
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
    val androidMain by getting {
      dependencies {
        dependencies {
          // AndroidX
          implementation(libs.androidx.activity.compose)
          implementation(libs.androidx.appcompat)
          implementation(libs.androidx.core.ktx)

          // Jetbrains Compose
          implementation(libs.jetbrains.compose.runtime)
          implementation(libs.jetbrains.compose.foundation)
          @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
          implementation(libs.jetbrains.compose.components.resources)

          implementation(libs.jetbrains.compose.material)
          implementation(libs.jetbrains.compose.material3)
          implementation(libs.jetbrains.compose.materialIcons)

          // test
          implementation(libs.compose.ui.preview)

          implementation(project(":shared"))
          implementation(project(":helper"))
          implementation(project(":helperCompose"))
          implementation(project(":helperPlatform"))
          implementation(project(":core"))
          implementation(project(":window"))
        }
      }
    }
  }
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
  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
      resValue("string", "appName", "Dweb Browser")
      applicationIdSuffix = null
      versionNameSuffix = null
    }
    debug {
      signingConfig = signingConfigs.getByName("debug")
      val userName = System.getProperty("user.name")
        .replace("[^a-zA-Z0-9]".toRegex(), "").lowercase()
      resValue("string", "appName", "Dev-$userName")
      applicationIdSuffix = ".kmp.$userName"
      versionNameSuffix = ".kmp.$userName"
    }
  }
}
