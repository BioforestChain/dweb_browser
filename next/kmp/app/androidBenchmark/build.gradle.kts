plugins {
  id("com.android.test")
  id("org.jetbrains.kotlin.android")
}

android {
  namespace = "info.bagen.dwebbrowser.benchmark"
  compileSdk = 34

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  kotlinOptions {
    jvmTarget = "1.8"
  }

  defaultConfig {
    minSdk = 28
    targetSdk = 34

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    // This benchmark buildType is used for benchmarking, and should function like your
    // release build (for example, with minification on). It"s signed with a debug key
    // for easy local/CI testing.
    create("benchmark1") {
      isDebuggable = true
      signingConfig = getByName("debug").signingConfig
      matchingFallbacks += listOf("release")
    }
  }

  targetProjectPath = ":androidApp"
  experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
  implementation(libs.androidx.test.junit)
  implementation(libs.androidx.test.runner)
  implementation(libs.androidx.test.rules)
  implementation(libs.androidx.test.espresso.core)
  implementation(libs.androidx.test.espresso.web)
  implementation(libs.androidx.test.uiautomator)
  implementation(libs.benchmark.macro.junit4)
  implementation(projects.browser)
}

androidComponents {
  beforeVariants(selector().all()) {
    it.enable = it.buildType == "benchmark1"
  }
}