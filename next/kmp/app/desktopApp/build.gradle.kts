import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
  alias(libs.plugins.kotlinxMultiplatform)
  alias(libs.plugins.jetbrainsCompose)
  id ("com.google.osdetector") version "1.7.3"
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

        // https://stackoverflow.com/questions/73187027/use-javafx-in-kotlin-multiplatform
        // As JavaFX have platform-specific dependencies, we need to add them manually
        val fxSuffix = when (osdetector.classifier) {
          "linux-x86_64" -> "linux"
          "linux-aarch_64" -> "linux-aarch64"
          "windows-x86_64" -> "win"
          "osx-x86_64" -> "mac"
          "osx-aarch_64" -> "mac-aarch64"
          else -> throw IllegalStateException("Unknown OS: ${osdetector.classifier}")
        }
        implementation("org.openjfx:javafx-base:19:${fxSuffix}")
        implementation("org.openjfx:javafx-graphics:19:${fxSuffix}")
        implementation("org.openjfx:javafx-controls:19:${fxSuffix}")
        implementation("org.openjfx:javafx-swing:19:${fxSuffix}")
        implementation("org.openjfx:javafx-web:19:${fxSuffix}")
        implementation("org.openjfx:javafx-media:19:${fxSuffix}")
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