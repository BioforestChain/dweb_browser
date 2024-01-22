plugins {
  id("kmp-library")
}

kotlin {
  kmpCommonTarget(project) {
    dependencies {
      implementation(libs.ktor.http)
      implementation(libs.ktor.client.encoding)

      implementation(projects.helper)
      implementation(projects.helperPlatform)
    }
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
      implementation(libs.ktor.io)
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
  kmpNodeJsTarget(project) {
    dependencies {
      implementation(libs.ktor.client.js)
    }
  }
}
