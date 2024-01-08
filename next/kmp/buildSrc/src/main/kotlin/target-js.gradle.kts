import org.gradle.accessors.dm.LibrariesForLibs

val libs = the<LibrariesForLibs>()

plugins {
  id("target-common")
}
rootProject.configureJsPlugins()
rootProject.configureYarn()