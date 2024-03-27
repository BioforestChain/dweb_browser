plugins {
//  alias(libs.plugins.kotlinMultiplatform)
//  alias(libs.plugins.androidLibrary)
  id("kmp-library")
}

/**
 * 参考 https://github.com/TeamDev-IP/JxBrowser-Gradle-Plugin/blob/main/src/main/kotlin/com/teamdev/jxbrowser/gradle/Environment.kt
 */
kotlin {
  kmpDesktopTarget(project) {
    dependencies {
      // api(libs.jxbrowser.cross.platform)
      /// 因为不允许交叉编译，所以我们每个平台只处理自己平台的就好了
      if (platform.isMac) {
        if (platform.isArm) {
          api(libs.jxbrowser.mac.arm)
        }
        if (platform.isX86) {
          api(libs.jxbrowser.mac)
        }
      } else if (platform.isWindows) {
        if (platform.is64) {
          api(libs.jxbrowser.win64)
        }
        if (platform.is32) {
          api(libs.jxbrowser.win32)
        }
      } else if (platform.isLinux) {
        if (platform.isArm) {
          api(libs.jxbrowser.linux64.arm)
        }
        if (platform.isX86) {
          api(libs.jxbrowser.linux64)
        }
      }
      api(libs.jxbrowser)
      api(libs.jxbrowser.swing)
    }
  }
}
