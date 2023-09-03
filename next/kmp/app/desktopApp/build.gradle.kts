import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
  alias(libs.plugins.kotlinxMultiplatform)
  alias(libs.plugins.jetbrainsCompose)
}

kotlin {
  jvm()
  sourceSets {
    val jvmMain by getting {
      dependencies {
        implementation(compose.desktop.currentOs)
        implementation(project(":shared"))
        implementation(project(":helper"))
        implementation(project(":helperCompose"))
      }
    }
  }
}

compose.desktop {
  application {
    mainClass = "MainKt"

    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = "org.dweb_browser.app.desktop"
      packageVersion = "1.0.0"
    }
  }
}