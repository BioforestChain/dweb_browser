plugins {
  id("kmp-compose")
}

kotlin {
  kmpCommonTarget(project) {
    @Suppress("OPT_IN_USAGE")
    applyHierarchy {
      common {
        group("skiko") {
          withIosTarget()
          withDesktopTarget()
        }
      }
    }
  }
  kmpComposeTarget(project) {
    dependencies {
      implementation(projects.helper)
      implementation(projects.helperPlatform)
      implementation(projects.pureIO)
      implementation(projects.pureHttp)

      // 加载图片 coil
      implementation(libs.coil.core)
      implementation(libs.coil.network.ktor)
      implementation(libs.coil.compose)
      implementation(libs.coil.svg)

      // ext

      implementation(libs.ktor.io)
      implementation(libs.ktor.server.websockets)
      implementation(libs.ktor.server.cio)

      implementation(libs.coil.gif)
      implementation(libs.coil.video)
    }
  }

  kmpAndroidTarget(project) {
  }
  kmpIosTarget(project) {
  }
  kmpDesktopTarget(project) {
  }
}

