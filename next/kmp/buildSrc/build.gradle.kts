import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
  `kotlin-dsl-base`
}

repositories {
  google {
    mavenContent {
      includeGroupByRegex(".*google.*")
      includeGroupByRegex(".*android.*")
    }
  }
  mavenCentral()
}

dependencies {
  implementation(libs.gradlePlugin.android)
  implementation(libs.gradlePlugin.kotlin)
}

tasks.withType<JavaCompile>().configureEach {
  sourceCompatibility = JavaVersion.VERSION_17.toString()
  targetCompatibility = JavaVersion.VERSION_17.toString()
}
tasks.withType<KotlinJvmCompile>().configureEach {
  compilerOptions.jvmTarget = JvmTarget.JVM_17
}
