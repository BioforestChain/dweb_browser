import org.dweb_browser.buildsrc.commonMobileTarget

plugins {
  alias(libs.plugins.kotlinxMultiplatform)
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.jetbrainsCompose)
  alias(libs.plugins.kotlinPluginSerialization)
}

kotlin {
  commonMobileTarget()

  sourceSets.commonMain.dependencies {
    implementation(libs.jetbrains.compose.runtime)
    implementation(libs.jetbrains.compose.foundation)
    implementation(libs.jetbrains.compose.components.resources)

    implementation(libs.jetbrains.compose.material3)
    implementation(libs.jetbrains.compose.materialIcons)

    implementation(projects.helper)
    implementation(projects.helperCompose)
    implementation(projects.helperPlatform)
    implementation(projects.core)
    implementation(projects.dWebView)
    implementation(projects.window)
  }
  sourceSets.commonTest.dependencies {
    implementation(kotlin("test"))
  }
  sourceSets.androidMain.dependencies {
    // Android 标准
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.animation.core.android)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    // Compose 相关
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.window)
    // Google.accompanist 相关
    implementation(libs.accompanist.webview)
    implementation(libs.accompanist.navigation.material)
    implementation(libs.accompanist.navigation.animation)
    implementation(libs.accompanist.systemui.controller)
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.insets.ui)
    implementation(libs.compose.ui.preview)
    //扫码核心库
    implementation(libs.camera.core)
    implementation(libs.camera.view)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.barcode)
  }
  sourceSets.iosMain.dependencies {
    implementation(projects.ziplib)
  }
}

android {
  namespace = "org.dweb_browser.browser"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
  }

  packaging {
    resources {
      excludes += "/META-INF/DEPENDENCIES"
    }
  }
  sourceSets["main"].apply {
    res.srcDirs("src/androidMain/res", "src/commonMain/res", "src/main/res")
  }
}
dependencies {
  implementation(project(":helperPlatform"))
}
