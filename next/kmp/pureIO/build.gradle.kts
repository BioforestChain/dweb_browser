plugins {
  id("kmp-library")
}

kotlin {
  val commonIoMain = sourceSets.create("commonIoMain") {
    dependencies {
//      implementation(libs.ktor.io)
    }
  }
  kmpAndroidTarget(libs) {
//    dependsOn(commonIoMain)
  }
  kmpIosTarget(libs) {
//    dependsOn(commonIoMain)
  }
  kmpBrowserJsTarget(libs) {
//    dependsOn(commonIoMain)
  }
  kmpNodeWasmTarget(libs){
  }
}

android {
  namespace = "org.dweb_browser.pure.io"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
  }
}

