plugins {
  id("kmp-compose")
  id(libs.plugins.androidApplication.get().pluginId)
}

kotlin {
  js(IR) {
    browser()
    binaries.executable()
  }
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmTarget.get()))
  }
  sourceSets {
    val jsMain by getting {
      dependencies {
        dependencies {
          // AndroidX
          implementation(libs.androidx.activity.compose)
          implementation(libs.androidx.appcompat)
          implementation(libs.androidx.core.ktx)
          implementation(libs.androidx.core.splashscreen)

          // Jetbrains Compose
          implementation(libs.jetbrains.compose.runtime)
          implementation(libs.jetbrains.compose.foundation)
          implementation(libs.jetbrains.compose.components.resources)

          implementation(libs.jetbrains.compose.material)
          implementation(libs.jetbrains.compose.material3)
          implementation(libs.jetbrains.compose.materialIcons)

          // Google.accompanist 相关
          implementation(libs.accompanist.webview)
          implementation(libs.accompanist.permissions)
          implementation(libs.accompanist.systemui.controller)


//          //扫码核心库
//          implementation(libs.camera.core)
//          implementation(libs.camera.view)
//          implementation(libs.camera.camera2)
//          implementation(libs.camera.lifecycle)
//          implementation(libs.camera.barcode)

          // test
          implementation(libs.compose.ui.preview)

          implementation(projects.shared)
          implementation(projects.helper)
          implementation(projects.helperCompose)
          implementation(projects.helperPlatform)
          implementation(projects.pureHttp)
          implementation(projects.pureImage)
          implementation(projects.core)
          implementation(projects.window)
          implementation(projects.sys)
          implementation(projects.browser)
          implementation(projects.dWebView)
        }
      }
    }
  }
}