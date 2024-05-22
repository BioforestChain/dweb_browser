import org.gradle.accessors.dm.LibrariesForLibs

val libs = the<LibrariesForLibs>()

plugins {
  id("org.jetbrains.compose")
  id("org.jetbrains.kotlin.plugin.compose")
}
