import org.gradle.accessors.dm.LibrariesForLibs
import java.util.Properties

val libs = the<LibrariesForLibs>()

plugins {
  id("kmp-library")
  id("com.teamdev.jxbrowser")
}

// 必须添加仓库，否则会导致一些依赖从jxbrowser仓库查找，从而获取失败
repositories {
  mavenCentral()
  google {
    mavenContent {
      includeGroupByRegex(".*android.*")
    }
  }
}

jxbrowser {
  version = libs.versions.jxbrowser.version.get()
  includePreviewBuilds = version.contains("eap")
}

kotlin {
  kmpDesktopTarget(project) {
    dependencies {
      api(jxbrowser.currentPlatform)
      api(jxbrowser.swing)
      if (libs.versions.jxbrowser.version.get().contains("eap")) {
        api(jxbrowser.compose)
      }
    }
  }
}
