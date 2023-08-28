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
  implementation(kotlin("stdlib"))
  implementation(libs.kotlinx.coroutines.core)
  testImplementation(libs.junit)

  api(libs.data.gson)
  api(libs.kotlin.serialization.json)
  api(libs.kotlin.serialization.cbor)

  /// 测试相关
  testImplementation(kotlin("test"))
  testImplementation(libs.test.kotlin.coroutines.test)
  testImplementation(libs.test.kotlin.coroutines.debug)
  testImplementation(platform(libs.test.junit.bom))
  testRuntimeOnly(libs.test.junit.platform.launcher) {
    because("Only needed to run tests in a version of IntelliJ IDEA that bundles older versions")
  }
  testImplementation(libs.test.junit.jupiter)

}

tasks.withType<Test> {
  useJUnitPlatform()
}