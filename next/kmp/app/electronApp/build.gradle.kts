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
//      implementation("org.jetbrains.kotlin-wrappers:kotlin-extensions:1.0.1-pre.689")

      implementation(kotlin("test"))
      implementation(project(":platformTest"))
      implementation(project(":jsCommon"))

    }
  }
}

project.fixJsInputFilePath()
/**
 * 在buildSrc执行后，当前这个project的tasks列出来了，我们可以基于这些task开始我们自己的扩展注册了
 */
gradle.projectsEvaluated {

  tasks.withType<NodeJsExec>().all {
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
tasks.register("jsElectronDevelopmentRunWithDemoReactAppDistribution") {
  doFirst {
    exec {
      workingDir("${rootProject.projectDir}")
      commandLine("./gradlew", ":demoReactApp:browserBrowserDistribution")

    }
  }
  doLast {
    exec {
      workingDir("${rootProject.projectDir}")
      commandLine("./gradlew", ":electronApp:jsElectronDevelopmentRun")
    }
  }
}

tasks.register("jsElectronDevelopmentRunWithDemoComposeAppDistribution") {
  doFirst {
    exec {
      workingDir("${rootProject.projectDir}")
      commandLine("./gradlew", ":demoComposeApp:browserBrowserDistribution")

    }
  }
  doLast {
    exec {
      workingDir("${rootProject.projectDir}")
      commandLine("./gradlew", ":electronApp:jsElectronDevelopmentRun")
    }
  }
}
