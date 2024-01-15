plugins {
  id("kmp-compose")
}

kotlin {
  kmpComposeTarget(project){
    dependencies{
      implementation(projects.helper)
      implementation(projects.helperPlatform)
      implementation(projects.pureIO)
    }
  }
  kmpAndroidTarget(project)
  kmpIosTarget(project)
}

android {
  namespace = "org.dweb_browser.helper.compose"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
  }
}

