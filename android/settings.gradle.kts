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

rootProject.name = "dweb-browser"
include(":app")
//include(":helper")
include("helper")
project(":helper").apply {
  projectDir = file("../next/kmp/helper")
}
include(":benchmark")
include(":MicroService")
include(":DWebView")
include(":BrowserUI")
include(":window")
include(":helperAndroid")
include("shared")
project(":shared").apply {
  projectDir = file("../next/kmp/shared")
  buildFileName  = "build-android.gradle.kts"
}
include("helperCompose")
project(":helperCompose").apply {
  projectDir = file("../next/kmp/helperCompose")
  buildFileName  = "build-android.gradle.kts"
}
