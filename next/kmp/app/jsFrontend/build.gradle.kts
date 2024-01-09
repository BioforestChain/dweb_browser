plugins {
  id("target-common")
  id("target-js")
}

beforeEvaluate {
  configureYarn()
}

kotlin {
  js(IR) {
    browser()
    useEsModules()
    binaries.library()
  }

  sourceSets {
    jsMain.dependencies {
      implementation(project.dependencies.enforcedPlatform(libs.kotlin.wrappers.bom))
      implementation(libs.kotlin.js)
      implementation(libs.kotlin.electron)

      implementation(projects.pureCrypto)
      implementation(npm("electron", "^28.1.1"))
    }
  }
}


// Electron tasks

tasks.withType<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask>()
  .configureEach {

  }