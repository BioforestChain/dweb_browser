import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.ByteArrayOutputStream

plugins {
  id("kmp-library")
}

kotlin {
  kmpNodeWasmTarget(libs) {

  }
}

android {
  namespace = "org.dweb_browser.helper.platform.node"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
  }
}