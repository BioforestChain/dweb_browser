plugins {
  id("kmp-compose")
}

kotlin {
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
  kmpDesktopTarget(project)
}
