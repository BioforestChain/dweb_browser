import com.version.manager.BuildConfig

plugins {
  id("java-library")
  id("org.jetbrains.kotlin.jvm")
  id("com.version.manager")
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
  version = "1.8.10"
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(BuildConfig.kotlinxCoroutines)
  testImplementation("junit:junit:4.13.2")

  /// 测试相关
  testImplementation(kotlin("test"))
  testImplementation(BuildConfig.testKotlinCoroutinesTest)
  testImplementation(BuildConfig.testKotlinCoroutinesDebug)
  testImplementation(platform(BuildConfig.testJUnitBom))
  testRuntimeOnly(BuildConfig.testJUnitPlatformLauncher) {
    because("Only needed to run tests in a version of IntelliJ IDEA that bundles older versions")
  }
  testImplementation(BuildConfig.testJUnitJupiter)

}

tasks.withType<Test> {
  useJUnitPlatform()
}