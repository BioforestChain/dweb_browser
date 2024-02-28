plugins {
  id("kmp-library")
}

kotlin {
  kmpCommonTarget(project) {
    dependencies {
      api(libs.ktor.io)
      api(libs.ktor.http)
      api(libs.ktor.client.encoding)

      implementation(projects.helper)
      implementation(projects.helperPlatform)
    }
    @Suppress("OPT_IN_USAGE")
    applyHierarchy {
      common {
        group("ktor") {
          withAndroidTarget()
          withIosTarget()
        }
      }
    }
  }

  val ktorMain by sourceSets.creating {
    dependencies {
      implementation(libs.ktor.server.core)
      implementation(libs.ktor.server.cio)
      implementation(libs.ktor.server.websockets)
    }
  }
  kmpAndroidTarget(project) {
    dependencies {
      implementation(libs.ktor.client.okhttp)
    }
  }
  kmpIosTarget(project) {
    dependencies {
      implementation(libs.ktor.client.darwin)
    }
  }
  kmpDesktopTarget(project) {
    dependencies {
      implementation(libs.ktor.client.okhttp)
    }
  }
}
