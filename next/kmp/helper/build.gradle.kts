import java.util.Properties

plugins {
  id("kmp-library")
}

// 定义编译的文件，用于保存 local.properties 文件中定义的参数，用于代码调用操作
val buildConfigPath = "build/classes/kotlin/source"

// 不能直接使用 Multiplatform 里面的 localProperties，那个有缓存，所以得这边自己创建一个
val gradleProperties = Properties()
val gradlePropertiesFile = rootDir.resolve("gradle.properties")
if (gradlePropertiesFile.exists()) {
  gradleProperties.load(gradlePropertiesFile.inputStream())
}

kotlin {
  kmpCommonTarget(project) {
    dependencies {
      api(libs.kotlinx.datetime)
      api(libs.ktor.http)
      api(libs.ktor.io)
    }
    @Suppress("OPT_IN_USAGE")
    applyHierarchy {
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
}

allprojects {
  afterEvaluate {
    // 判断编译的时候是否传入了 -PreleaseBuild=true，表示是脚本执行
    val isReleaseBuild = hasProperty("releaseBuild") && property("releaseBuild") == "true"

    // 在 gradle sync，或者编译的时候，会执行当前code
    // 这个是创建一个配置文件
    val content = if (!isReleaseBuild) { // 增加判断，如果是脚本执行会默认增加 -PreleaseBuild=true
      val stringBuffer = StringBuffer()
      gradleProperties.forEach { (key, value) ->
        if (key.toString().startsWith("dweb-") && value.toString().isNotEmpty()) { // 只针对 dweb- 开头的内容
          println("QAQ: key = $key, value = $value")
          stringBuffer.append("\n    \"${key.toString().replace("\\", "\\\\")}\"") // 追加 key
          stringBuffer.append(" to ")
          stringBuffer.append("\"${value.toString().replace("\\", "\\\\")}\",") // 追加 value
        }
      }
"""
package org.dweb_browser.helper

object CommonBuildConfig {
  val switchMaps: Map<String, String> = mapOf($stringBuffer
  )
}
""".trimIndent()
    } else {
"""
package org.dweb_browser.helper

object CommonBuildConfig {
  val switchMaps: Map<String, String> = mapOf()
}
""".trimIndent()
    }



    file("$buildConfigPath/CommonBuildConfig.kt").also { // 创建父级目录
      if (it.exists()) it.deleteRecursively()
      it.parentFile.mkdirs()
    }.writeText(content)
  }
}