plugins {
  id("mobile-compose-target")
}

kotlin {
  sourceSets.commonMain.dependencies {
    api(libs.jetbrains.compose.runtime)
    api(libs.jetbrains.compose.foundation)
    api(libs.jetbrains.compose.components.resources)
    api(libs.kotlinx.atomicfu)
    api(libs.ktor.server.cio)
    api(libs.ktor.client.cio)
    api(libs.ktor.client.encoding)
    api(libs.ktor.server.websockets)

    implementation(libs.jetbrains.compose.material3)

    implementation(projects.helper)
  }
  sourceSets.commonTest.dependencies {
    implementation(kotlin("test"))
  }
  sourceSets.androidMain.dependencies {
    // Android Runtime
    api(libs.androidx.core.ktx)
    api(libs.androidx.activity)
    api(libs.androidx.activity.ktx)
    api(libs.androidx.activity.compose)
    api(libs.androidx.appcompat)
    api(libs.androidx.animation.core.android)
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.accompanist.systemui.controller)
    api(libs.google.material)
    // 加载图片 coil
    api(libs.coil.core)
    api(libs.coil.compose)
    api(libs.coil.svg)
    api(libs.coil.video)
    api(libs.coil.gif)
  }
  sourceSets.iosMain.dependencies {
    api(libs.ktor.client.darwin)
    api(projects.helperPlatformIos)
  }
}

android {
  namespace = "org.dweb_browser.helper.platform"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
  }

  sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}

