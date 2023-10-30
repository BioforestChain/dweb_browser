plugins {
  alias(libs.plugins.kotlinxMultiplatform)
  alias(libs.plugins.androidLibrary)
}

kotlin {
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
    iosX64(), iosArm64(), iosSimulatorArm64()
  ).forEach {
    it.binaries.framework {
      baseName = "DwebMdns"
    }
  }

  applyDefaultHierarchyTemplate()

  sourceSets.commonMain.dependencies {
    api(kotlin("stdlib"))
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.atomicfu)
    api(libs.ktor.network)
    implementation(project(":helper"))
  }
  sourceSets.commonTest.dependencies {
    implementation(kotlin("test"))
    implementation(libs.test.kotlin.coroutines.test)
    implementation(libs.test.kotlin.coroutines.debug)
  }
}
android {
  namespace = "org.dweb_browser.mdns"
  compileSdk = libs.versions.compileSdkVersion.get().toInt()
  defaultConfig {
    minSdk = libs.versions.minSdkVersion.get().toInt()
  }
}