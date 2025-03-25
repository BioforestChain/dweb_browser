import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  id("target-common")
}


val libs = the<LibrariesForLibs>()

kotlin {
  kmpCommonTarget(project) {
    dependencies {
      implementation(libs.squareup.okio)
      implementation(libs.kotlinx.datetime)
    }
  }
  kmpDesktopTarget(project) {
    dependencies {
      api(libs.java.jna)
    }
  }
  sourceSets.all {
    languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
  }
}
