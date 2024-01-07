
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask

plugins {
  id("target-common")
}

kotlin {
  js(IR) {
    nodejs()
    useEsModules()
    generateTypeScriptDefinitions()
    binaries.executable()
  }
  configureNodejs()
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
fun Project.createElectronProject(
  taskName: String,
  mainFileName: Provider<String>,
  outputDirectory: Provider<File>,
): TaskProvider<Task> = tasks.register(taskName, Task::class) {
  doFirst {
    val electronMjs = File(outputDirectory.get(), "package.json")
    val newJson = electronMjs.readText().replace("\n}\n",""",
      "electron":{
      
      }
    }
    """.trimIndent())
    electronMjs.writeText(newJson)
  }
}

fun Project.createElectronExec(
  npmProject: NpmProject,
  mainFile: Provider<RegularFile>,
  taskName: String,
  taskGroup: String?
): TaskProvider<Exec> {

  val outputDirectory = provider { npmProject.dir }
  val mainFileName = mainFile.map { it.asFile.toPath().relativize(npmProject.dir.toPath()).toString() }

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


//rootProject.the<NodeJsRootExtension>().apply {
//    nodeVersion = "21.5.1"
//    nodeDownloadBaseUrl = "https://nodejs.org/download/release"
//}
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

tasks.withType<KotlinNpmInstallTask>().configureEach {

  println("KotlinNpmInstallTask: $name")
  args.add("--ignore-engines")
  doFirst {
    exec {
      println("in '${workingDir.absolutePath}', run `yarn set version latest`.")
      commandLine("yarn", "set", "version", "latest")
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