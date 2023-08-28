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
include(":helper")
include(":benchmark")
include(":MicroService")
include(":DWebView")
include(":BrowserUI")
include(":window")
include(":helperAndroid")
