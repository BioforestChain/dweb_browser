import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec

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
      implementation(kotlin("stdlib"))
      implementation(libs.kotlinx.coroutines.core)
      implementation(libs.kotlinx.atomicfu)
      implementation(libs.kotlinx.io)

      implementation(project.dependencies.enforcedPlatform(libs.kotlin.wrappers.bom))
      implementation(libs.kotlin.js)
      implementation(libs.kotlin.electron)

      implementation(projects.pureCrypto)
      implementation(projects.jsFrontend)
      implementation(npm("electron", "^28.1.1"))
      implementation(npm("${rootProject.name}-${projects.jsFrontend.name}", "workspace:^"))
      implementation(npm("${rootProject.name}-${projects.wasmBackend.name}-wasm-js", "workspace:^"))
    }
  }
}


// Electron tasks

tasks.withType<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask>()
  .configureEach {

  }


/**
 * 在buildSrc执行后，当前这个project的tasks列出来了，我们可以基于这些task开始我们自己的扩展注册了
 */
gradle.projectsEvaluated {
  println("projectsEvaluated: $name")

  tasks.forEach {
    println("task: $name:${it.name} (${it.description})")
  }

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
    ) {
      val mode = when {
        name.contains("Development") -> "Development";
        else -> "Production"
      }
      dependsOn(":" + projects.jsFrontend.name + ":jsBrowser${mode}LibraryPrepare")
      dependsOn(":" + projects.wasmBackend.name + ":wasmJsNode${mode}LibraryPrepare")
    }

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