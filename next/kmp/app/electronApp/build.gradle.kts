import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject

plugins {
  id("target-common")
  id("target-js")
}

beforeEvaluate {
  configureYarn()
}

kotlin {
  js(IR) {
    nodejs()
    useEsModules()
    generateTypeScriptDefinitions()
    binaries.executable()
  }
  sourceSets {
    jsMain.dependencies {
      implementation(project.dependencies.enforcedPlatform(libs.kotlin.wrappers.bom))
      implementation(libs.kotlin.js)
      implementation(libs.kotlin.electron)

      implementation(projects.pureCrypto)
      implementation(npm("electron", "^28.1.1"))
    }
  }
}


// Electron tasks

tasks.withType<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask>()
  .configureEach {

  }
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
  dependsOn(":electronApp:jsPackageJson")
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
  taskGroup: String?
): TaskProvider<Exec> {

  val outputDirectory = provider { npmProject.dir }
  val mainFileName =
    mainFile.map { it.asFile.toPath().relativize(npmProject.dir.toPath()).toString() }

  val electronFileTask = createElectronProject(
    taskName = "${taskName}CreateProject",
    mainFileName = mainFileName,
    outputDirectory = outputDirectory,
  )

  return tasks.register(taskName, Exec::class) {
    dependsOn(electronFileTask)
    dependsOn(":kotlinNpmInstall")
    dependsOn(":kotlinUpgradeYarnLock")

    group = taskGroup

    description = "Executes with Electron"

    val newArgs = mutableListOf<String>()

    executable = "yarn"

    newArgs += listOf("run", "electron", ".")
    newArgs.add("electron")
    newArgs.add(".")

    args = newArgs

    workingDir = outputDirectory.get()
  }
}


/**
 * 在buildSrc执行后，当前这个project的tasks列出来了，我们可以基于这些task开始我们自己的扩展注册了
 */
gradle.projectsEvaluated {
  println("beforeProject: $name")

  tasks.forEach {
    println("tasks.all: ${it.name} (${it.description})")
  }

  println("NodeJsExec:${tasks.withType<NodeJsExec>().asMap.keys.joinToString(",")}")
  println("Exec:${tasks.withType<Exec>().asMap.keys.joinToString(",")}")
  println("tasks:${tasks.asMap.keys.joinToString(",")}")

  tasks.withType<NodeJsExec>().all {
    println("node task:$name")
    println("exec project:${npmProject.dir.absolutePath}")
    println("exec args:${args?.joinToString(",")}")
    println("exec nodeArgs:${nodeArgs.joinToString(",")}")
    println("exec inputFileProperty:${inputFileProperty.get().asFile.absolutePath}")
    inputFileProperty.fileValue(
      File(
        inputFileProperty.get().asFile.absolutePath.replace(
          Regex("\\.js$"), ".mjs"
        )
      )
    )

    println("exec inputFileProperty:${inputFileProperty.get().asFile.absolutePath}")
    println("exec commandLine:${commandLine.joinToString(",")}")

    val electronExecTask = createElectronExec(
      npmProject,
      inputFileProperty,
      name.replace("Node", "Electron"),
      group
    )

    electronExecTask.configure {
      dependsOn(
        project.provider { this@all.taskDependencies }
      )
    }
  }
}

//tasks.withType<KotlinJsTest>().all {
//    val electronExecTask = createElectronExec(
//        inputFileProperty,
//        name.replace("Node", "Electron"),
//        group
//    )
//
//    electronExecTask.configure {
//        dependsOn (
//            project.provider { this@all.taskDependencies }
//        )
//    }
//
//    tasks.withType<KotlinTestReport> {
//        dependsOn(electronExecTask)
//    }
//}

//tasks.withType<NodeJsExec>().all {
//  val electronExecTask = createElectronExec(
//    inputFileProperty,
//    name.replace("Node", "Electron"),
//    group
//  )
//
//  electronExecTask.configure {
//    dependsOn(
//      project.provider { this@all.taskDependencies }
//    )
//  }
//}