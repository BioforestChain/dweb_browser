import org.gradle.api.JavaVersion
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.kpm.external.ExternalVariantApi
import org.jetbrains.kotlin.gradle.kpm.external.project

fun KotlinMultiplatformExtension.commonMobileTarget(frameworkName: String? = null) {
  androidTarget {
    compilations.all {
      kotlinOptions {
        jvmTarget = JvmTarget.JVM_17.target
      }
    }
  }
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_17.toString()))
  }

  listOf(
    iosX64(), iosArm64(), iosSimulatorArm64()
  ).forEach {
    if (frameworkName != null) {
      it.binaries.framework {
        baseName = frameworkName
        isStatic = true
      }
    }
  }

  applyDefaultHierarchyTemplate()
}

@OptIn(ExternalVariantApi::class)
fun KotlinMultiplatformExtension.mobileXCFrameworkTarget(frameworkName: String? = null) {
  androidTarget {
    compilations.all {
      kotlinOptions {
        jvmTarget = JvmTarget.JVM_17.target
      }
    }
  }
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(JavaVersion.VERSION_17.toString()))
  }

//  val defPath = project.file("${project.rootDir}/buildSrc/src/main/nativeInterop/cinterop")
//  val frameworkPath = project.file("${project.rootDir}/buildSrc/src/main/libs/dweb_platform_kit_ios.xcframework")

  listOf(
    iosX64(), iosArm64(), iosSimulatorArm64()
  ).forEach {
    if (frameworkName != null) {
      it.binaries.framework {
        baseName = frameworkName
        isStatic = true
      }
    }

//    it.compilations.getByName("main") {
//      cinterops.create("dweb_platform_kit_ios") {
//        defFile("$defPath/dweb_platform_kit_ios.def")
//
//        if (it.targetName == "iosArm64") {
//          compilerOpts("-framework", "DwebIOS", "-F$frameworkPath/ios-arm64/")
//        } else {
//          compilerOpts(
//            "-framework",
//            "DwebIOS",
//            "-F$frameworkPath/ios-arm64_x86_64-simulator/"
//          )
//        }
//        extraOpts += listOf("-compiler-option", "-fmodules")
//      }
//    }
//
//    it.binaries.all {
//      if (it.targetName == "iosArm64") {
//        linkerOpts("-framework", "DwebIOS", "-F$frameworkPath/ios-arm64/")
//      } else {
//        linkerOpts("-framework", "DwebIOS", "-F$frameworkPath/ios-arm64_x86_64-simulator/")
//      }
//    }
  }

  applyDefaultHierarchyTemplate()
}
