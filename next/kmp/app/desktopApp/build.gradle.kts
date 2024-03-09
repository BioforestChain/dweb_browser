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

  kmpDesktopTarget(project) {
    dependencies {
      implementation(projects.platformDesktop)
      implementation(libs.jcef.maven)
    }
  }
}

compose.desktop {
  application {
//    mainClass = "MainKt"
    mainClass = "StartKt"

    args += listOf(
      "--add-opens java.desktop/sun.awt=ALL-UNNAMED",
      "--add-opens java.desktop/sun.lwawt=ALL-UNNAMED",
      "--add-opens java.desktop/sun.lwawt.macosx=ALL-UNNAMED"
    )

    jvmArgs += listOf(
      "--add-opens java.desktop/sun.awt=ALL-UNNAMED",
      "--add-opens java.desktop/sun.lwawt=ALL-UNNAMED",
      "--add-opens java.desktop/sun.lwawt.macosx=ALL-UNNAMED",
    )

    nativeDistributions {
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
      packageName = "info.bagen.dwebbrowser"
      packageVersion = "1.0.0"
    }
  }
}

afterEvaluate {
  tasks.withType<JavaExec> {
    jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
    jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
    jvmArgs("--add-exports", "java.base/java.lang=ALL-UNNAMED")
    jvmArgs("--add-exports", "java.desktop/sun.awt=ALL-UNNAMED")
    jvmArgs("--add-exports", "java.desktop/sun.java2d=ALL-UNNAMED")
  }
}