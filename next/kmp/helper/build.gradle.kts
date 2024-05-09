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
          withDesktopTarget()
          withAndroidTarget()
        }
        withIosTarget()
      }
    }
  }

  val jvmMain = sourceSets.creating {
    dependencies {
      api(libs.org.slf4j.log4j12)
    }
  }

  kmpAndroidTarget(project) {
  }
  kmpIosTarget(project) {
    dependencies {
      implementation(libs.squareup.okio)
    }
  }
  kmpDesktopTarget(project) {
  }
}