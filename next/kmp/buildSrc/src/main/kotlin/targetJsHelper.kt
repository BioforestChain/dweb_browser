import org.gradle.api.Project
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension


fun Project.configureJsPlugins() {
  println("configureJsPlugins")
  // 配置 插件
  plugins.withType<NodeJsRootPlugin> {
    println("NodeJsRootPlugin")
    // 不要下载 nodejs 可执行文件，默认环境里头已经有了
    the<NodeJsRootExtension>().apply {
      download = false
      nodeVersion = "21.5.1"
      nodeDownloadBaseUrl = "https://nodejs.org/download/release"
    }

    // 下载 yarn 可执行文件，默认环境里头已经有了
    the<YarnRootExtension>().apply {
      println("YarnRootExtension")
      download = false
      ignoreScripts = false// 这个不要开，yarn4不支持。开这个意味着 'ignore either your local postinstall script'
    }
  }
}

fun Project.configureYarn() {
  println("configureYarn for $name")
//  tasks.forEach {
//    println("configureYarn: $name:${it.name}")
//  }
  tasks.withType<KotlinNpmInstallTask>().configureEach {
    println("KotlinNpmInstallTask: ${this@configureYarn.name}:$name")
    doFirst {
      val yarnEnvFile = yarnLock.parentFile.resolve(".env.yarn")
      if (!yarnEnvFile.exists()) {
        exec {
          workingDir = yarnLock.parentFile
          println("in '${workingDir.absolutePath}', run `yarn set version stable`.")
          commandLine("yarn", "set", "version", "latest")
        }
        yarnEnvFile.writeText(
          """
          ELECTRON_MIRROR="https://npmmirror.com/mirrors/electron/"
        """.trimIndent()
        )
      }
    }
  }
}