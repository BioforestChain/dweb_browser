plugins {
  id("kmp-library")
}

// 定义编译的文件，用于保存 local.properties 文件中定义的参数，用于代码调用操作
val buildConfigPath = "build/generated/kmpBuildConfig/kotlin"

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

  val jvmMain = sourceSets.creating {
    dependencies {
      api(libs.org.slf4j.log4j12)
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

val localProperties = localProperties()

allprojects {
  afterEvaluate {
    // 在 gradle sync，或者编译的时候，会执行当前code
    // 这个是创建一个配置文件
    val stringBuffer = StringBuffer()
    localProperties.forEach { (key, value) ->
      if (key == "jxbrowser.license.key" || key == "sdk.dir") return@forEach // 这两个特殊作用，不是作为开关的，不添加进去
      println("QAQ: key = $key, value = $value")
      stringBuffer.append("\n    \"${key.toString().replace("\\", "\\\\")}\"") // 追加 key
      stringBuffer.append(" to ")
      stringBuffer.append("\"${value.toString().replace("\\", "\\\\")}\",") // 追加 value
    }

    val content = """
package org.dweb_browser.helper

object CommonBuildConfig {
  val switchMaps: Map<String, String> = mapOf($stringBuffer
  )
}
    """.trimIndent()

    file("$buildConfigPath/CommonBuildConfig.kt").also { // 创建父级目录
      if (it.exists()) it.deleteRecursively()
      it.parentFile.mkdirs()
    }.writeText(content)
  }
}