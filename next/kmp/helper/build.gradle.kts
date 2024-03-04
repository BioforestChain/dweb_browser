plugins {
  id("kmp-library")
}

kotlin {
  kmpCommonTarget(project) {
    dependencies {
      api(libs.kotlinx.datetime)
      api(libs.ktor.http)
      api(libs.ktor.io)
    }
    @Suppress("OPT_IN_USAGE")
    applyHierarchy {
      common {
        group("commonJs") {
          withJs()
        }
        group("jvm") {
          withJvm()
          withAndroidTarget()
        }
        withIosTarget()
      }
    }
  }

  kmpAndroidTarget(project) {
  }
  kmpIosTarget(project) {
  }
  kmpDesktopTarget(project) {
  }
}