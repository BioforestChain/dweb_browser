plugins {
  id("java-library")
  id("org.jetbrains.kotlin.jvm")
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
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
}