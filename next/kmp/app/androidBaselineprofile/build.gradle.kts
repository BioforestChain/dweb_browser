plugins {
  id("com.android.test")
  id("org.jetbrains.kotlin.android")
  alias(libs.plugins.androidxBaselineprofile)
}

android {
  namespace = "org.dweb_browser.baselineprofile"
  compileSdk = 34

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions {
    jvmTarget = "17"
  }

  defaultConfig {
    minSdk = 28
    targetSdk = 34

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    missingDimensionStrategy("abi", "forArm64", "release")
  }

  targetProjectPath = ":androidApp"

}

// This is the configuration block for the Baseline Profile plugin.
// You can specify to run the generators on a managed devices or connected devices.
baselineProfile {
  useConnectedDevices = true
}

dependencies {
  implementation(libs.androidx.test.junit)
  implementation(libs.androidx.test.espresso.core)
  implementation(libs.androidx.test.uiautomator)
  implementation(libs.benchmark.macro.junit4)
}

androidComponents {
  onVariants { v ->
    val artifactsLoader = v.artifacts.getBuiltArtifactsLoader()
    @Suppress("UnstableApiUsage")
    v.instrumentationRunnerArguments.put(
      "targetAppId",
      v.testedApks.map { artifactsLoader.load(it)?.applicationId!! }
    )
  }
}