pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}
val includeKmp = { kmpPath: String ->
  include(kmpPath)
  project(":$kmpPath").apply {
    projectDir = file("../next/kmp/$kmpPath")
    buildFileName = "build-android.gradle.kts"
  }
}


rootProject.name = "dweb-browser"
include(":app")
include(":benchmark")
//include(":MicroService")
include(":DWebView")
include(":BrowserUI")
//include(":window")
//include(":helperAndroid")
includeKmp("shared")
includeKmp("helper")
includeKmp("helperCompose")
includeKmp("helperPlatform")
includeKmp("window")
includeKmp("microService")
