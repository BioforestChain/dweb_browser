plugins {
  id("target-common")
  id("target-js")
}

beforeEvaluate {
  configureYarn()
}

kotlin {
  kmpBrowserJsTarget(project) {
    js {
      binaries.library()
    }
    dependencies {
    }
  }
}


// Electron tasks

tasks.withType<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask>()
  .configureEach {

  }