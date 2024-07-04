plugins {
  id("kmp-library")
}

kotlin {
  kmpCommonTarget(project) {
    dependencies {
      api(libs.squareup.okio)
      api(libs.squareup.okio.fakefilesystem)

      implementation(projects.helper)
    }

    @Suppress("OPT_IN_USAGE")
    applyHierarchyTemplate {
      common {
        group("jvm") {
          withDesktopTarget()
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
