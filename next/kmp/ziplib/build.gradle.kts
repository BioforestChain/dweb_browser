plugins {
  alias(libs.plugins.kotlinxMultiplatform)
  alias(libs.plugins.androidLibrary)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  targetHierarchy.default()

  androidTarget {
    compilations.all {
      kotlinOptions {
        jvmTarget = libs.versions.jvmTarget.get()
      }
    }
  }

  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmTarget.get()))
  }

  listOf(
    iosX64(),
    iosArm64(),
    iosSimulatorArm64()
  ).forEach {
    it.binaries.framework {
      baseName = "ziplib"
    }
    val main by it.compilations.getting
    main.cinterops.create("ziplib") {
      includeDirs(project.file("src/nativeInterop/cinterop/headers/ziplib"), project.file("src/libs/${it.targetName}"))
    }
  }

  sourceSets {
    all {
      languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
    }

    val commonMain by getting {
      dependencies {
        //put your multiplatform dependencies here
        api(libs.kotlinx.atomicfu)
        implementation(libs.squareup.okio)
        implementation(libs.kotlinx.datetime)
      }
    }
    val commonTest by getting {
      dependencies {
        kotlin("test")
      }
    }

    val androidMain by getting {
      dependsOn(commonMain)
      dependencies {
        implementation(libs.java.jna)
      }
    }

    val iosX64Main by getting
    val iosArm64Main by getting
    val iosSimulatorArm64Main by getting
    val iosMain by getting {
      dependsOn(commonMain)
      iosX64Main.dependsOn(this)
      iosArm64Main.dependsOn(this)
      iosSimulatorArm64Main.dependsOn(this)
    }
  }
}

android {
  namespace = "org.dweb_browser.ziplib"
  compileSdk = 34
  defaultConfig {
    minSdk = 29
    consumerProguardFiles("consumer-rules.pro")
  }
}
