import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import java.io.File

/**
 * 为一个项目提供 "electron" 的属性设置
 *
 * 在 electronApp 的 package.json 创建之后，写入 "electron" 属性
 */
fun Project.createElectronProject(
  taskName: String,
  mainFileName: Provider<String>,
  outputDirectory: Provider<File>,
): TaskProvider<Task> = tasks.register(taskName, Task::class) {
//  dependsOn(":electronApp:jsPackageJson")
  doFirst {
    val electronMjs = File(outputDirectory.get(), "package.json")
    val newJson = electronMjs.readText().replace(
      "\n}\n", """,
      "electron":{
      
      }
    }
    """.trimIndent()
    )
    electronMjs.writeText(newJson)
  }
}

/**
 * 为一个项目提供 electron 启动指令
 *
 * 从而可以执行 ./gradlew :PROJECT:electronRun 的指令
 */
fun Project.createElectronExec(
  npmProject: NpmProject,
  mainFile: Provider<RegularFile>,
  taskName: String,
  taskGroup: String? = null,
  configurationAction: Action<in Exec>? = null
): TaskProvider<Exec> {
  val outputDirectory = provider { npmProject.dir.get().asFile }
  val mainFileName =
    mainFile.map { it.asFile.toPath().relativize(npmProject.dir.get().asFile.toPath()).toString() }

  val electronFileTask = createElectronProject(
    taskName = "${taskName}CreateProject",
    mainFileName = mainFileName,
    outputDirectory = outputDirectory,
  )

  println("createElectronExec/tasks.register $taskName")
  return tasks.register(taskName, Exec::class) {
    dependsOn(electronFileTask)
    dependsOn(":kotlinNpmInstall")
    dependsOn(":kotlinUpgradeYarnLock")

    if (taskGroup != null) {
      group = taskGroup
    }

    description = "Executes with Electron"

    executable = "yarn"
    setArgs(listOf("run", "electron", "."))

    workingDir = outputDirectory.get()

    configurationAction?.invoke(this)
  }
}