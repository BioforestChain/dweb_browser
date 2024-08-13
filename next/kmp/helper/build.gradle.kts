import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import java.util.Properties

plugins {
  id("kmp-library")
}

// 定义编译的文件，用于保存 local.properties 文件中定义的参数，用于代码调用操作
val buildConfigPath = "build/classes/kotlin/source"

// 不能直接使用 Multiplatform 里面的 localProperties，那个有缓存，所以得这边自己创建一个
val localProperties = Properties()
val localPropertiesFile = rootDir.resolve("local.properties")
if (localPropertiesFile.exists()) {
  localProperties.load(localPropertiesFile.inputStream())
}

kotlin {

  kmpCommonTarget(project) {
    dependencies {
      api(libs.kotlinx.datetime)
      api(libs.ktor.http)
      api(libs.ktor.io)
      api(projects.libMixCompression)
    }
  }

  sourceSets.jvmMain {
    dependencies {
      //解压缩
      implementation(libs.apache.common.compress)
//      api(libs.org.slf4j.log4j12)
    }
  }

  kmpAndroidTarget(project) {
  }
  kmpIosTarget(project) {
    dependencies {
      implementation(libs.squareup.okio)
    }
  }
  kmpDesktopTarget(project) {
  }

  // 增加
  sourceSets["commonMain"].kotlin.srcDir(buildConfigPath)
  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  applyHierarchyPlatformTemplate {
    common {
      group("commonJs") {
        withJs()
      }
      group("jvm") {
        withDesktopTarget()
        withAndroidTarget()
      }
      withIosTarget()
    }
  }
}

allprojects {
  afterEvaluate {
    // 判断编译的时候是否传入了 -PreleaseBuild=true，表示是脚本执行
    val isReleaseBuild = hasProperty("releaseBuild") && property("releaseBuild") == "true"
    val keyValuePairCodes = when {
      isReleaseBuild -> emptyList()
      else -> localProperties.mapNotNull { (key, value) ->
        val keyStr = key.toString()
        val valStr = value.toString()
        // 只针对 dweb- 开头的内容
        when {
          keyStr.startsWith("dweb-") && valStr.isNotBlank() -> {
            """
              "$keyStr" to "$valStr"
            """.trimIndent()
          }

          else -> null
        }
      }
    }

    // 在 gradle sync，或者编译的时候，会执行当前code
    // 这个是创建一个配置文件
    val sourceCode = """
    package org.dweb_browser.helper
    
    public object CommonBuildConfig {
      public val switchMaps: Map<String, String> = mapOf(
      ${keyValuePairCodes.joinToString(",\n    ")}
      )
    }
    """.trimIndent()

    file("$buildConfigPath/CommonBuildConfig.kt").also { // 创建父级目录
      if (it.exists()) it.deleteRecursively()
      it.parentFile.mkdirs()
    }.writeText(sourceCode)
  }
}