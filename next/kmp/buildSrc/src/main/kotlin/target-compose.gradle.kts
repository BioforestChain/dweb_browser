import org.gradle.accessors.dm.LibrariesForLibs

plugins {
  id("org.jetbrains.compose")
}

val libs = the<LibrariesForLibs>()
private val composeVersion: String = libs.versions.jetbrains.compose.version.get()
infix fun String.belong(domain: String) = this == domain || this.startsWith("$domain.")
configurations.all {
  resolutionStrategy.eachDependency {
    with(requested) {
      if (group belong "org.jetbrains.compose" && !(group belong "org.jetbrains.compose.compiler")) {
        if (version != composeVersion) {
          println("resolutionStrategy ${group}:${name}:${version}")
          useVersion(composeVersion)
        }
      }
    }
  }
}