import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  id("target-common")
}


val libs = the<LibrariesForLibs>()

kotlin {
//  @OptIn(ExperimentalKotlinGradlePluginApi::class)
//  compilerOptions {
//    languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
//    apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
//  }
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
