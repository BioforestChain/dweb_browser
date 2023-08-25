@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
  id("java-library")
  alias(libs.plugins.kotlinJvm)
  alias(libs.plugins.kotlinPluginSerialization)
}

java {
  sourceCompatibility = JavaVersion.valueOf(libs.versions.javaVersion.get())
  targetCompatibility = JavaVersion.valueOf(libs.versions.javaVersion.get())
}

dependencies {
  implementation(libs.kotlinx.coroutines.core)

  api(platform(libs.http4k.bom))
  api(libs.http4k.core)
  api(libs.http4k.multipart)
  api(libs.http4k.client.apache)
  api(libs.ktor.server.websockets)
  api(libs.ktor.server.cio)
  api(libs.ktor.client.cio)

  implementation(libs.data.moshi.pack)

  /// 测试相关
  testImplementation(kotlin("test"))
  testImplementation(libs.test.kotlin.coroutines.test) {
    exclude("org.jetbrains.kotlinx", "kotlinx-coroutines-debug")
  }
  testImplementation(libs.test.kotlin.coroutines.debug)
  testImplementation(platform(libs.test.junit.bom))
  testRuntimeOnly(libs.test.junit.platform.launcher) {
    because("Only needed to run tests in a version of IntelliJ IDEA that bundles older versions")
  }
  testImplementation(libs.test.junit.jupiter)

  implementation(project(mapOf("path" to ":helper")))
}

tasks.withType<Test> {
  useJUnitPlatform()
}