plugins {
  id("kmp-compose")
}

kotlin {
  kmpCommonTarget(project) {
    @Suppress("OPT_IN_USAGE")
    applyHierarchy {
      common {
        group("jvm") {
          withAndroidTarget()
          withDesktopTarget()
        }
      }
    }
  }
  kmpComposeTarget(project) {
    dependencies {
      implementation(projects.helper)
      implementation(projects.helperCompose)
      implementation(projects.helperPlatform)
      implementation(projects.pureIO)
      implementation(projects.pureCrypto)
      implementation(projects.pureHttp)
    }
  }
  kmpAndroidTarget(project) {
    dependencies {
      implementation(projects.libMultipart)
    }
  }
  kmpIosTarget(project) {
    dependencies {
      implementation(projects.libMultipart)
    }
  }
  kmpDesktopTarget(project) {
    dependencies {
      implementation(projects.libMultipart)
    }
  }
}
