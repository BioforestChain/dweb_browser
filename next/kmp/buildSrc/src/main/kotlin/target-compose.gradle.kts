import org.gradle.accessors.dm.LibrariesForLibs

val libs = the<LibrariesForLibs>()

plugins {
  id("org.jetbrains.compose")
}

private val composeVersion: String = libs.versions.jetbrains.compose.version.get()
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