import org.gradle.accessors.dm.LibrariesForLibs

val libs = the<LibrariesForLibs>()

plugins {
  id("target-common")
}
rootProject.configureJsPlugins()
rootProject.configureYarn()

private val ktorWasmVersion: String = libs.versions.ktor.wasm.version.get()
configurations.all {
  resolutionStrategy.eachDependency {
    with(requested) {
//      if (group belong "org.jetbrains.compose" && !(group belong "org.jetbrains.compose.compiler")) {
//        if (version != composeVersion) {
////          println("resolutionStrategy ${group}:${name}:${version}")
//          useVersion(composeVersion)
//        }
//      }
    }
  }
}