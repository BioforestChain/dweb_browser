import org.gradle.accessors.dm.LibrariesForLibs

val libs = the<LibrariesForLibs>()

plugins {
  id("kmp-library")
  id("com.teamdev.jxbrowser")
}

// 必须添加仓库，否则会导致一些依赖从jxbrowser仓库查找，从而获取失败
repositories {
  google {
    mavenContent {
      includeGroupByRegex(".*google.*")
      includeGroupByRegex(".*android.*")
      includeGroupByRegex(".*androidx.*")
    }
  }
  mavenCentral()
  gradlePluginPortal()
}

jxbrowser {
  version = libs.versions.jxbrowser.version.get()
  includePreviewBuilds = version.contains("eap")
}

kotlin {
  //#region 不添加的话，高版本的 Android Gradle Plugin 会有异常提示
  kmpAndroidTarget(project)
  //#endregion

  kmpDesktopTarget(project) {
    dependencies {
      if (System.getProperty("usePinpit") == "true") {
        api(jxbrowser.mac)
      } else {
        api(jxbrowser.currentPlatform)
      }

      api(jxbrowser.compose)
    }
  }
}
