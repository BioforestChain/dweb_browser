plugins {
  alias(libs.plugins.androidLibrary)
  alias(libs.plugins.kotlinAndroid)
  alias(libs.plugins.kotlinPluginSerialization)
}

android {
  namespace = "org.dweb_browser.microservice"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()

  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
  }
  compileOptions {
    sourceCompatibility = JavaVersion.valueOf(libs.versions.javaVersion.get())
    targetCompatibility = JavaVersion.valueOf(libs.versions.javaVersion.get())
  }
  kotlinOptions {
    jvmTarget = libs.versions.jvmTarget.get()
  }
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
  api(libs.data.gson)

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

  implementation(project(":helper"))
}

tasks.withType<Test> {
  useJUnitPlatform()
}