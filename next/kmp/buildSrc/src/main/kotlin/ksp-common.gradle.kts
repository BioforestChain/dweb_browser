import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
  id("com.google.devtools.ksp")
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
  if (name != "kspCommonMainKotlinMetadata") {
    dependsOn("kspCommonMainKotlinMetadata")
  }
}