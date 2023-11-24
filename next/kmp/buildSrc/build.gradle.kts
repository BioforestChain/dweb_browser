plugins {
  `kotlin-dsl`
}

repositories {
  maven("https://maven.pkg.jetbrains.space/public/p/compose/dev") {
    mavenContent {
      includeGroupByRegex(".*compose.*")
    }
  }

  google {
    mavenContent {
      includeGroupByRegex(".*google.*")
      includeGroupByRegex(".*android.*")
    }
  }

  mavenCentral()
  gradlePluginPortal()
}

//tasks.register("clean", Delete::class) {
//  delete(rootProject.buildDir)
//}

dependencies {
  implementation(libs.gradlePlugin.jetbrainsCompose)
  implementation(libs.gradlePlugin.android)
  implementation(libs.gradlePlugin.kotlin)
  implementation(kotlin("serialization:${libs.versions.kotlin.version}"))
}
