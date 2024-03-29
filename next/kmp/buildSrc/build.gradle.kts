import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

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
  // maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
}

//tasks.register("clean", Delete::class) {
//  delete(rootProject.buildDir)
//}

dependencies {
  implementation(libs.gradlePlugin.jetbrainsCompose)
  implementation(libs.gradlePlugin.android)
  implementation(libs.gradlePlugin.kotlin)
  //   implementation(kotlin("serialization:${libs.versions.kotlin.version}"))
  implementation(kotlin("serialization:1.9.22"))
  implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

  // jxbrowser
  implementation(libs.gradlePlugin.jxbrowser)
}
tasks.withType<JavaCompile>().configureEach {
  println("JavaCompile: $name")
  val javaVersion = libs.versions.jvmTarget.get()
  sourceCompatibility = javaVersion
  targetCompatibility = javaVersion
}
tasks.withType<KotlinJvmCompile>().configureEach {
  println("KotlinJvmCompile: $name")
  val jvmTarget = JvmTarget.fromTarget(libs.versions.jvmTarget.get())
  compilerOptions.jvmTarget.set(jvmTarget)
}
