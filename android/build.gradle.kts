import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.diagnostics.deprecationError0
import kotlin.system.exitProcess

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.androidApplication) apply false
  alias(libs.plugins.kotlinAndroid) apply false
  alias(libs.plugins.androidLibrary) apply false
  alias(libs.plugins.kotlinJvm) apply false
  alias(libs.plugins.kspAndroid) apply false
  alias(libs.plugins.jetbrainsCompose) apply false
  alias(libs.plugins.kotlinxMultiplatform) apply false
}

/**
 * 为了在根目录增加一个 libs.versions.toml 链接，方便查看版本信息
 */
tasks.register("createSymbolicLink") {
  doFirst {
    val externalFile = File("${project.rootDir}/../next/kmp/gradle/libs.versions.toml") // 替换为实际的文件路径
    val targetFile = File("${project.rootDir}/libs.versions.toml")
    if (Os.isFamily(Os.FAMILY_WINDOWS)) {
      project.exec {
        commandLine("cmd", "/c", "mklink", targetFile.absolutePath, externalFile.absolutePath)
        //commandLine("cmd", "/c", "dir")
        //exitProcess(1)
      }
    } else {
      project.exec {
        commandLine("ln", "-s", externalFile.absolutePath, targetFile.absolutePath)
        //exitProcess(1)
      }
    }
  }
}