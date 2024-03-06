import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
  id("target-common")
  id("target-js")
}

beforeEvaluate {
  configureYarn()
}

kotlin {
  js(IR) {
    binaries.library()
    browser {  }

    nodejs{

    }
    sourceSets{
      val jsMain by getting{
        dependencies{
          implementation(kotlin("stdlib"))
          implementation(libs.kotlinx.coroutines.core)
          implementation(libs.kotlinx.atomicfu)
          implementation(libs.kotlinx.io)
          implementation(libs.kotlin.js)
          implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-js:1.6.2")
        }
      }
      val jsTest by getting{
        dependencies{
          implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-js:1.6.2")
          implementation(kotlin("test"))
          implementation(kotlin("test-js"))
          implementation(project(":platformTest"))
        }
      }
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