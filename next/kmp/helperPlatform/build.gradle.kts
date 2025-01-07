import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
  id("kmp-compose")
  id("ksp-common")
}

val buildOutputDir = "build/generated/ksp/metadata/commonMain/kotlin"
kotlin {
  kmpCommonTarget(project, {
    srcDirs(buildOutputDir)
  }) {
    dependencies {
      implementation(projects.pureCrypto)
    }
  }
  kmpComposeTarget(project) {
    dependencies {
      implementation(projects.helper)
      /**
       * 注意，helperPlatform 不可以依赖 helperCompose
       * 因为 helperCompose是面向UI的，helperPlatform是面向Application，因此更加底层
       *
       * 但是helperPlatform中包含 helper.compose 这样的一个 namespace 文件夹，
       * 所以如果有需要，可以将 helperCompose 中的代码迁移到 helperPlatform 中来
       */
    }
  }
  kmpAndroidTarget(project) {
    dependencies {
      implementation(libs.google.material)
    }
  }
  kmpIosTarget(project) {
  }
  kmpDesktopTarget(project) {
  }
  sourceSets.create("skikoMain") {
    dependencies {
      implementation(libs.dweb.keychainstore)
    }
  }

  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  applyHierarchyPlatformTemplate {
    common {
      group("skiko") {
        withIosTarget()
        withDesktopTarget()
      }
      withAndroidTarget()
    }
  }
}

val kspArguments = mutableMapOf<String, String>()

project.properties.forEach { (key, value) ->
  if (key == "rootDir" || key == "releaseBuild") {
    kspArguments += key to value.toString()
  }
}

ksp {
  kspArguments.forEach { (key, value) ->
    arg(key, value)
  }
}

dependencies {
  kspAll(projects.helperKsp)
}

//#region 确保修改local.properties能正确生成新的CommonBuildConfig
tasks.register("deleteCommonBuildConfig") {
  delete(project.projectDir.resolve(buildOutputDir))
}

tasks.withType<KotlinCompilationTask<*>>().matching { it.name == "kspCommonMainKotlinMetadata" }
  .configureEach {
    dependsOn("deleteCommonBuildConfig")
  }
//#endregion