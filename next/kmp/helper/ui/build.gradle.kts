plugins {
  id("kmp-library")
}

kotlin {
  kmpCommonTarget(project)
  kmpBrowserJsTarget(project) {
    dependencies {
      api(libs.kotlinx.datetime)
      api(libs.ktor.http)
      api(libs.ktor.io)
    }
  }

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  applyHierarchyPlatformTemplate {
    common {
      group("commonJs") {
        withJs()
      }
    }
  }
}