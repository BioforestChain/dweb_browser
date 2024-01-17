plugins {
  id("kmp-library")
}

kotlin {
  kmpCommonTarget(project) {
    dependencies {
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
      implementation(libs.ktor.http)
      implementation(libs.ktor.server.core)
      implementation(libs.ktor.client.encoding)

      implementation(libs.ktor.io)
      implementation(libs.ktor.server.cio)
      implementation(libs.ktor.server.websockets)
    }
  }
  kmpAndroidTarget(project) {
    dependencies {
      implementation(libs.ktor.client.cio)
    }
  }
  kmpIosTarget(project) {
    dependencies {
      implementation(libs.ktor.client.darwin)
    }
  }
  kmpNodeJsTarget(project) {

  }
}
