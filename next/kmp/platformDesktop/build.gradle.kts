plugins {
//  alias(libs.plugins.kotlinMultiplatform)
//  alias(libs.plugins.androidLibrary)
  id("kmp-library")
}

/**
 * 参考 https://github.com/TeamDev-IP/JxBrowser-Gradle-Plugin/blob/main/src/main/kotlin/com/teamdev/jxbrowser/gradle/Environment.kt
 */
private val ARCH_X64 = setOf("amd64", "x86_64")
private val ARCH_X86 = setOf("x86", "i386")
private val ARCH_ARM = setOf("aarch64", "arm")
kotlin {
  kmpDesktopTarget(project) {
    dependencies {
      // api(libs.jxbrowser.cross.platform)
      val osName = System.getProperty("os.name")
      val osArch = System.getProperty("os.arch")
      println("QWQ platformDesktop info: osName=$osName osArch=$osArch")
      /// 因为不允许交叉编译，所以我们每个平台只处理自己平台的就好了
      if (osName.startsWith("Mac")) {
        api(libs.jxbrowser.mac.arm)
        api(libs.jxbrowser.mac)
      } else if (osName.startsWith("Windows")) {
        api(libs.jxbrowser.win64)
      } else if (osName.startsWith("Linux")) {
        api(libs.jxbrowser.linux64.arm)
        api(libs.jxbrowser.linux64)
      }
      api(libs.jxbrowser)
      api(libs.jxbrowser.swing)
    }
  }
}
