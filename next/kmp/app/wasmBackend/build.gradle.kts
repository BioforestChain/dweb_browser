plugins {
  id("target-common")
  id("target-js")
}

beforeEvaluate {
  configureYarn()
}

kotlin {
  @Suppress("OPT_IN_USAGE")
  wasmJs {
    // 这里默认就是 useEsModules
    nodejs()
    applyBinaryen()
    binaries.library()
  }
  sourceSets {
    val wasmJsMain by getting {
      dependencies {
//        implementation(projects.pureCrypto)
        implementation(npm("electron", "^28.1.1"))
      }
    }
//    jsMain.dependencies {
//      implementation(project.dependencies.enforcedPlatform(libs.kotlin.wrappers.bom))
//      implementation(libs.kotlin.js)
//      implementation(libs.kotlin.electron)
//
//      implementation(projects.pureCrypto)
//      implementation(npm("electron", "^28.1.1"))
//    }
  }
}

