import java.util.Properties

plugins {
//  alias(libs.plugins.kotlinMultiplatform)
//  alias(libs.plugins.androidLibrary)
  id("kmp-library")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
  localProperties.load(localPropertiesFile.inputStream())
}

val disabledApps = (localProperties.getProperty("app.disable") as String).split(",")
  .map { it.trim().lowercase() }
val enableAndroidApp = !disabledApps.contains("android")
val enableIosApp = !disabledApps.contains("ios")
val enableElectronApp = !disabledApps.contains("electron")
val enableDesktop = !disabledApps.contains("desktop")
val enableLibs = enableAndroidApp || enableIosApp || enableDesktop

println("""
  -----------------------
  enableAndroidApp: $enableAndroidApp
  enableIosApp: $enableIosApp
  enableElectronApp: $enableElectronApp
  enableDesktop: $enableDesktop
  enableLibs: $enableLibs
  -----------------------------------------------
""".trimIndent())

kotlin {
  kmpCommonTarget(project) {
    dependencies {
      implementation(libs.test.kotlin.coroutines.test)
    }
  }
  if(enableAndroidApp) kmpAndroidTarget(project)
  if(enableIosApp)kmpIosTarget(project)
  if(enableElectronApp) kmpNodeJsTarget(project)
  if(enableDesktop) kmpDesktopTarget(project)
}
