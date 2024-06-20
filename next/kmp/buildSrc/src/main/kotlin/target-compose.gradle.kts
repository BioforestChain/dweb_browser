import org.gradle.accessors.dm.LibrariesForLibs

val libs = the<LibrariesForLibs>()

plugins {
  id("org.jetbrains.compose")
  kotlin("plugin.compose")
}
