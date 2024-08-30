plugins {
  id("kmp-library")
}

kotlin {
  //#region 不添加的话，高版本的 Android Gradle Plugin 会有异常提示
  kmpAndroidTarget(project)
  jvm()
  sourceSets {
    jvmMain {
      dependencies {
        implementation(libs.google.devtools.ksp)
      }
      kotlin.srcDir("src/jvmMain/kotlin")
      resources.srcDir("src/jvmMain/resources")
    }
  }
}

