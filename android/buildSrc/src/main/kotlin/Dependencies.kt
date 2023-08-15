package info.bagen.version

import org.gradle.api.JavaVersion

object BuildVersion {
  const val compileSdkVersion = 33
  const val buildToolsVersion = "29.0.2"
  const val minSdkVersion = 28
  const val targetSdkVersion = 33
  const val versionCode = 6
  const val versionName = "1.1.5"

  const val kotlinVersion = "1.9.0"
  const val kotlinCompilerExtensionVersion = "1.5.1"
  const val jvmTarget = "17"
  val javaVersion = JavaVersion.VERSION_17
}