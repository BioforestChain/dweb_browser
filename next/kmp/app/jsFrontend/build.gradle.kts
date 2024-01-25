import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
  id("target-common")
  id("target-js")
  id("org.jetbrains.compose")
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
      api("org.jetbrains.kotlin-wrappers:kotlin-react:18.2.0-pre.687")
      api("org.jetbrains.kotlin-wrappers:kotlin-react-dom:18.2.0-pre.687")
      api(compose.runtime)
      api(compose.ui)
      api(compose.foundation)
      api(compose.material)
      @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
      api(compose.components.resources)
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