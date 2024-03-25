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
      targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
      packageName = "DwebBrowser"
      packageVersion = "1.0.0"

      val iconsRoot =
        project.file("${rootProject.rootDir}/app/desktopApp/src/desktopMain/res/icons")
      val macExtraPlistKeys = """
        <key>CFBundleURLTypes</key>
        <array>
          <dict>
            <key>CFBundleURLName</key>
            <string>info.bagen.dwebbrowser</string>
            <key>CFBundleURLSchemes</key>
            <array>
              <string>dweb</string>
            </array>
          </dict>
        </array>
      """
      windows {
        iconFile.set(iconsRoot.resolve("win/icon.ico"))
        menuGroup = "Developer"
        dirChooser = true
        shortcut = true

        // see https://wixtoolset.org/documentation/manual/v3/howtos/general/generate_guids.html
        upgradeUuid = "2f6b36ed-8691-4c5e-a645-6aaf8e9ec32d"
      }

      macOS {
        iconFile.set(iconsRoot.resolve("mac/icon.icns"))
        bundleID = "info.bagen.dwebbrowser"
        setDockNameSameAsPackageName = true

        infoPlist {
          extraKeysRawXml = macExtraPlistKeys
        }
      }
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
