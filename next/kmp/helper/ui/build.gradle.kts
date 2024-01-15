plugins {
  id("kmp-library")
}

kotlin {
  val commonJsMain by sourceSets.creating {
    dependsOn(sourceSets.commonMain.get())
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