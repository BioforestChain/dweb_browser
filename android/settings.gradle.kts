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
    maven("https://androidx.dev/storage/compose-compiler/repository/")
  }
  versionCatalogs {
    create("libs") {
      from(files("../next/kmp/gradle/libs.versions.toml"))
    }
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
includeKmp("shared")
includeKmp("helper")
includeKmp("helperCompose")
includeKmp("helperPlatform")
includeKmp("window")
includeKmp("core")
includeKmp("DWebView")
includeKmp("browser")
includeKmp("ziplib")
includeKmp("sys")
