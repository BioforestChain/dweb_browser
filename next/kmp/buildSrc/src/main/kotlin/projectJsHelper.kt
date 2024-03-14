import org.gradle.api.Project
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension


private val configureJsPluginsProjects = mutableSetOf<Project>()
fun Project.configureJsPlugins() {
  if (configureJsPluginsProjects.contains(this)) {
    return
  }
  configureJsPluginsProjects += this
  println("configureJsPlugins")
  // 配置 插件
  plugins.withType<NodeJsRootPlugin> {
    println("NodeJsRootPlugin")
    // 不要下载 nodejs 可执行文件，默认环境里头已经有了
    the<NodeJsRootExtension>().apply {
//      download = false
//      nodeVersion = "21.5.1"
//      nodeDownloadBaseUrl = "https://nodejs.org/download/release"

      nodeVersion = "22.0.0-v8-canary202401082b4956f9f0"
      nodeDownloadBaseUrl = "https://nodejs.org/download/v8-canary"
    }

    // 下载 yarn 可执行文件，默认环境里头已经有了
    the<YarnRootExtension>().apply {
      println("YarnRootExtension")
      download = false
      ignoreScripts = false// 这个不要开，yarn4不支持。开这个意味着 'ignore either your local postinstall script'
    }
  }
}

private val configKotlinNpmInstallTasks = mutableSetOf<Project>()
fun Project.configureYarn() {
  if (configKotlinNpmInstallTasks.contains(this)) {
    return
  }
  configKotlinNpmInstallTasks.add(this)
  println("configureYarn for $name")
  tasks.withType<KotlinNpmInstallTask>().configureEach {
    println("KotlinNpmInstallTask: ${this@configureYarn.name}:$name args=$args")
    args += "install"
    doFirst {
      val yarnLock = additionalFiles.files.first { it.name == "yarn.lock" }
      val yarnEnvFile = yarnLock.parentFile.resolve(".env.yarn")
      if (!yarnEnvFile.exists()) {
        /// 设置版本号4+
        exec {
          workingDir = yarnLock.parentFile
          println("in '${workingDir.absolutePath}', run `yarn set version stable`.")
          commandLine("yarn", "set", "version", "stable")
        }
        /// 写入环境变量文件
        yarnEnvFile.writeText(
          """
          ELECTRON_MIRROR="https://npmmirror.com/mirrors/electron/"
        """.trimIndent()
        )

        /// 配置 yarnrc
        val yarnRcFile = yarnLock.parentFile.resolve(".yarnrc.yml")
        if (yarnRcFile.exists()) {
          val content = yarnRcFile.readText()
          if (!content.contains("nodeLinker:")) {
            yarnRcFile.writeText("$content\nnodeLinker: node-modules")
          }
        }
      }
    }
  }
}