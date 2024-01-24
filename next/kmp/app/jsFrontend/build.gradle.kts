import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
  id("target-common")
  id("target-js")
}

beforeEvaluate {
  configureYarn()
}

kotlin {
  kmpBrowserJsTarget(project) {
    js{
      binaries.library()
    }
    dependencies {
      implementation("org.jetbrains.kotlin-wrappers:kotlin-react:18.2.0-pre.687")
      implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom:18.2.0-pre.687")
    }
  }
}


tasks.register("stage") {
    dependsOn("build")
}


// Electron tasks

tasks.withType<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask>()
  .configureEach {

  }