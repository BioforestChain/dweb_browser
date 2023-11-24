import org.gradle.api.JavaVersion
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

fun KotlinMultiplatformExtension.mobileTarget() {
  androidTarget {
    compilations.all {
      kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
      }
    }
  }
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_17.toString()))
  }

  iosX64()
  iosArm64()
  iosSimulatorArm64()

  applyDefaultHierarchyTemplate()
}
