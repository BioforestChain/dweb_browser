import com.dd.plist.NSDictionary
import com.dd.plist.NSString
import de.mobanisto.pinpit.desktop.application.tasks.DistributableAppTask
import org.gradle.internal.os.OperatingSystem

plugins {
  id("target-common")
  id("target-compose")
  id("com.android.application")
  id("desktop-pinpit-package")
}

apply("./build-version.gradle.kts")

val appVersion: String by extra

kotlin {
  androidTarget()

  kmpComposeTarget(project) {
    val usePinpit = System.getProperty("usePinpit")
    dependencies {
      if (usePinpit == "true") {
        if(OperatingSystem.current().isMacOsX) {
          implementation(compose.desktop.macos_x64)
        } else {
          implementation("org.jetbrains.compose.desktop:desktop-jvm-windows-arm64:${libs.versions.jetbrains.compose.version}")
        }
      } else {
        implementation(compose.desktop.currentOs)
      }
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
      implementation(libs.ktor.server.core)
      implementation(libs.ktor.server.netty)
    }
  }
}

android {
  namespace = "org.dweb_browser.pinpit"
  compileSdk = 34

  defaultConfig {
    minSdk = 28
    targetSdk = 34
  }
}

val iconsRoot =
  project.file("${rootProject.rootDir}/app/desktopApp/src/desktopMain/res/icons")

pinpit.desktop {
  application {
    mainClass = "MainKt"

    // 用于打包应用时注入key
    jvmArgs += listOf(
      "-Djxbrowser.license.key=${getJxBrowserLicenseKey()}",
      "-Ddwebbrowser.version=$appVersion",
    )

    nativeDistributions {
      jvmVendor = "adoptium"
      jvmVersion = "17.0.12+7"
      includeAllModules = true

      packageName = "DwebBrowser"
      packageVersion = appVersion
      description = "Dweb Browser - Deploy your own `.dweb` domain on your local device"
      vendor = "BNGJ"
      copyright = "2022-2024 BFChain"

      windows {
        iconFile.set(iconsRoot.resolve("win/icon.ico"))
        menuGroup = "Developer"
        dirChooser = true
        shortcut = true
        console = false
        // 打包msi必须再此处设置packageVersion，否则会提示找不到
        packageVersion = appVersion

        // see https://wixtoolset.org/documentation/manual/v3/howtos/general/generate_guids.html
        upgradeUuid = "2f6b36ed-8691-4c5e-a645-6aaf8e9ec32d"

        msi {
          arch = "x64"
        }

        msi {
          arch = "arm64"
        }

        distributableArchive {
          format = "zip"
          arch = "x64"
        }

        distributableArchive {
          format = "zip"
          arch = "arm64"
        }
      }

      macOS {
        iconFile.set(iconsRoot.resolve("mac/icon.icns"))
        bundleID = "com.instinct.bfexplorer"
        appCategory = "public.app-category.productivity"
        setDockNameSameAsPackageName = true

        distributableArchive {
          format = "zip"
          arch = "x64"
        }
        distributableArchive {
          format = "zip"
          arch = "arm64"
        }
      }
    }
  }
}

// 用于启动桌面应用时注入key
afterEvaluate {
  tasks.withType<JavaExec>() {
    localProperties().copyTo(systemProperties)
    System.getProperties().copyTo(systemProperties)
    // 注入应用版本
    systemProperties["dwebbrowser.version"] = appVersion

    jvmArgs("--add-opens", "java.desktop/java.awt=ALL-UNNAMED")
  }

  //# region 改写Info.plist
  tasks.withType<DistributableAppTask> {
    doLast {
      val infoPlistFile = project.file("${rootProject.rootDir}/app/desktopApp/Info.plist")
      val infoPlistTargetFile =
        project.file("${rootProject.rootDir}/app/desktopApp/build/pinpit/binaries/main-default/macos/x64/distributableApp/DwebBrowser.app/Contents/Info.plist")

      val infoPlistNSDictionary = NSDictionary()
      infoPlistNSDictionary["CFBundleShortVersionString"] = NSString(appVersion)
      infoPlistNSDictionary["CFBundleVersion"] = NSString(appVersion)
      infoPlistNSDictionary["CFBundleIconFile"] =
        NSString(iconsRoot.resolve("mac/icon.icns").name)
      infoPlistFileReplace(
        infoPlistFile.toPath(),
        infoPlistTargetFile.toPath(),
        infoPlistNSDictionary
      )
    }

    onlyIf { OperatingSystem.current().isMacOsX }
  }
  //# endregion

  //# region mac x86_64 sign
  val macSignTask = tasks.register<MacSignTask>("runMacSignTask") {
    dependsOn("pinpitCreateDefaultDistributableMacosX64")

    javaRuntimeVersion.set(libs.versions.jvmTarget.get().toInt())
    macPackageName.set("DwebBrowser")
    macBundleId.set("com.instinct.bfexplorer")
    macEntitlementsFile.set(project.file("${rootProject.rootDir}/app/desktopApp/default-entitlements.plist"))
    macRuntimeEntitlementsFile.set(project.file("${rootProject.rootDir}/app/desktopApp/runtime-entitlements.plist"))
  }

  tasks.named("pinpitPackageDefaultDistributableZipMacosX64") {
    dependsOn(macSignTask)
  }
  //# endregion
}




