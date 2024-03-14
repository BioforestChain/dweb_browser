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
      implementation(projects.shared) // 用于导入 resources 文件夹
      implementation(projects.helper)
      implementation(projects.helperCompose)
      implementation(projects.helperPlatform)
      implementation(projects.pureIO)
      implementation(projects.pureHttp)
      implementation(projects.pureImage)
      implementation(projects.core)
      implementation(projects.window)
      implementation(projects.sys)
      implementation(projects.browser)
      implementation(projects.dwebview)
    }
  }
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

(tasks.findByName("desktopRun") as JavaExec?)?.apply {
  println("QAQ classpath=$classpath")
  println("QAQ mainClass=$mainClass")

  classpath = sourceSets["main"].runtimeClasspath
  mainClass = System.getProperty("MainKt")

  jvmArgs("-Ddebug=true")
}