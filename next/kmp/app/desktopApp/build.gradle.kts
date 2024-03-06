import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
  id("target-compose")
  id("target-common")
}

kotlin {
  kmpComposeTarget(project) {
    dependencies {
      implementation(compose.desktop.currentOs)
    }
  }

  kmpDesktopTarget(project)
}

compose.desktop {
  application {
    mainClass = "MainKt"

    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = "info.bagen.dwebbrowser"
      packageVersion = "1.0.0"
    }
  }
}