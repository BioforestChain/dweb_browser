plugins {
  id("kmp-library")
}

kotlin {
  val commonJsMain by sourceSets.creating {
  }
  kmpBrowserJsTarget(project) {
    dependsOn(commonJsMain)
    dependencies {
      api(libs.kotlinx.datetime)
      api(libs.ktor.http)
      api(libs.ktor.io)
    }
  }
}