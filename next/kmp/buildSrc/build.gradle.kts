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
      includeGroupByRegex(".*androidx.*")
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
  implementation(libs.gradlePlugin.composeCompiler)
  implementation(libs.gradlePlugin.android)
  implementation(libs.gradlePlugin.kotlin)
  implementation(libs.gradlePlugin.serialization)
  implementation(libs.gradlePlugin.pinpit)
  implementation(libs.gradlePlugin.ksp)
  implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

  // jxbrowser
  implementation(libs.gradlePlugin.jxbrowser)
  // plist 解析
  implementation(libs.dd.plist)
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
