import org.gradle.accessors.dm.LibrariesForLibs

val libs = the<LibrariesForLibs>()

plugins {
  id("target-common")
}

// 配置 nodejs
rootProject.configureNodejs()
