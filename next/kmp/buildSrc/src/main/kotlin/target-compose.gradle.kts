import org.gradle.accessors.dm.LibrariesForLibs

val libs = the<LibrariesForLibs>()

plugins {
  id("org.jetbrains.compose")
}

private val composeVersion: String = libs.versions.jetbrains.compose.version.get()
/// 如果是dev模式，可能会存在 alpha 或者 dev 版本号冲突的问题
if (composeVersion.contains("dev")) {
  configurations.all {
    resolutionStrategy.eachDependency {
      with(requested) {
        if (group belong "org.jetbrains.compose" && !(group belong "org.jetbrains.compose.compiler")) {
          if (version != composeVersion) {
//          println("resolutionStrategy ${group}:${name}:${version}")
            useVersion(composeVersion)
          }
        }
      }
    }
  }
}