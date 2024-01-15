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
  }
  kmpAndroidTarget(project)
  kmpIosTarget(project)
  val commonJsMain by sourceSets.creating {
    dependsOn(sourceSets.commonMain.get())
  }

  kmpNodeJsTarget(project) {
    dependsOn(commonJsMain)
  }
}
