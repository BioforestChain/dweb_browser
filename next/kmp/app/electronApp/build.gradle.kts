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
//    useEsModules()
    generateTypeScriptDefinitions()
    binaries.executable()
  }
  sourceSets {
    jsMain.dependencies {
      implementation(kotlin("stdlib"))
      implementation(libs.kotlinx.coroutines.core)
      implementation(libs.kotlinx.atomicfu)
      implementation(libs.kotlinx.io)

      implementation(libs.kotlin.js)
      implementation(libs.kotlin.electron)

//      implementation(projects.pureCrypto)
//      implementation(projects.jsFrontend)
      implementation(npm("electron", "^28.1.1"))
      implementation(npm("${rootProject.name}-${projects.jsFrontend.name}-browser", "workspace:^"))
//      implementation(npm("${rootProject.name}-${projects.jsBackend.name}-node", "workspace:^"))
      // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-serialization-json-js
      implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-js:1.6.2")
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

  tasks.withType<NodeJsExec>().all {
    if (npmProject.target.compilations.first().kotlinOptions.useEsClasses) {
      inputFileProperty.fileValue(
        File(
          inputFileProperty.get().asFile.absolutePath.replace(
            Regex("\\.js$"), ".mjs"
          )
        )
      )
    }

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
//      dependsOn(":" + projects.jsFrontend.name + ":browserBrowser${mode}LibraryPrepare")
//      dependsOn(":" + projects.jsBackend.name + ":nodeNode${mode}LibraryPrepare")
    }

    electronExecTask.configure {
      dependsOn(
        project.provider { this@all.taskDependencies }
      )
    }
  }
}

/**
 * - 把jsFrontend打包到electronApp的resources/jsFrontEnd目录
 * - 执行:electronApp:jsElectronDevelopmentRun任务
 */
tasks.register("jsElectronDevelopmentRunWithJsFrontendDistribution"){
  doFirst{
    exec{
      workingDir("${rootProject.projectDir}")
      commandLine("./gradlew", ":jsFrontend:browserBrowserDistribution")

    }
  }
  doLast {
    exec {
      workingDir("${rootProject.projectDir}")
      commandLine("./gradlew", ":electronApp:jsElectronDevelopmentRun")
    }
  }
}
