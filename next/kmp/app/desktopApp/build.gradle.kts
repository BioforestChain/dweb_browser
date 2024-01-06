

plugins {
  id("target-common")
}

kotlin {
  js(IR) {
    nodejs()
    useEsModules()
    binaries.executable()
  }
  configureNodejs()
  sourceSets {
    jsMain.dependencies {
      implementation(project.dependencies.enforcedPlatform(libs.kotlin.wrappers.bom))
      implementation(libs.kotlin.js)
      implementation(libs.kotlin.electron)

      implementation(projects.pureCrypto)
      implementation(npm("electron","^28.1.1"))
    }
  }
}


// Electron tasks

tasks.withType<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask>().configureEach {

}

fun Project.createElectronProject(
    taskName: String,
    mainFileName: Provider<String>,
    outputDirectory: Provider<File>,
): TaskProvider<Task> = tasks.register(taskName, Task::class) {
    doFirst {
        val electronMjs = File(outputDirectory.get(), "package.json")
        electronMjs.writeText(getElectronPackageJson(mainFileName.get()))
    }
}

fun Project.createElectronExec(
    nodeMjsFile: RegularFileProperty,
    taskName: String,
    taskGroup: String?
): TaskProvider<Exec> {

    val outputDirectory = nodeMjsFile.map { it.asFile.parentFile }
    val mainFileName = nodeMjsFile.map { it.asFile.name }

    val electronFileTask = createElectronProject(
        taskName = "${taskName}CreateProject",
        mainFileName = mainFileName,
        outputDirectory = outputDirectory,
    )

    return tasks.register(taskName, Exec::class) {
        dependsOn(electronFileTask)

        group = taskGroup

        description = "Executes with Electron"

        val newArgs = mutableListOf<String>()

        executable = "pnpm"

        newArgs.add("electron")
        newArgs.add(".")

        args = newArgs

        workingDir = outputDirectory.get()
    }
}


tasks.withType<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask>().configureEach {
    args.add("--ignore-engines")
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
//}
fun getElectronPackageJson(mainFileName: String): String = """
{
  "main": "./$mainFileName",
  "appBundleId": "com.instinct.macApp1st",
  "name": "@dweb-browser/desktop",
  "companyName": "Bnqkl, Inc.",
  "appCopyright": "Copyright © 2023 Bnqkl, Inc.",
  "productName": "Dweb Browser",
  "homepage": "https://github.com/BioforestChain/dweb_browser",
  "version": "0.2.0",
  "description": "Distributed web browser",
  "license": "MIT",
  "config": {
    "electron_mirror": "https://npm.taobao.org/mirrors/electron/"
  },
  "scripts": {
    "start": "electron ./"
  },
  "author": "Bnqkl Dweb Team",
  "exports": {
    ".": {
      "require": "./script/index.js",
      "import": "./esm/index.js"
    },
    "./core": {
      "import": "./esm/core/index.js",
      "require": "./script/core/index.js"
    },
    "./core/*": {
      "import": "./esm/core/*.js",
      "require": "./script/core/*.js"
    },
    "./core/*.ts": {
      "import": "./esm/core/*.js",
      "require": "./script/core/*.js"
    },
    "./std": {
      "import": "./esm/std/index.js",
      "require": "./script/std/index.js"
    },
    "./std/*": {
      "import": "./esm/std/*.js",
      "require": "./script/std/*.js"
    },
    "./std/*.ts": {
      "import": "./esm/std/*.js",
      "require": "./script/std/*.js"
    },
    "./browser": {
      "import": "./esm/browser/index.js",
      "require": "./script/browser/index.js"
    },
    "./browser/*": {
      "import": "./esm/browser/*.js",
      "require": "./script/browser/*.js"
    },
    "./browser/*.ts": {
      "import": "./esm/browser/*.js",
      "require": "./script/browser/*.js"
    },
    "./sys": {
      "import": "./esm/sys/index.js",
      "require": "./script/sys/index.js"
    },
    "./sys/*": {
      "import": "./esm/sys/*.js",
      "require": "./script/sys/*.js"
    },
    "./sys/*.ts": {
      "import": "./esm/sys/*.js",
      "require": "./script/sys/*.js"
    },
    "./helper": {
      "import": "./esm/helper/index.js",
      "require": "./script/helper/index.js"
    },
    "./helper/*": {
      "import": "./esm/helper/*.js",
      "require": "./script/helper/*.js"
    },
    "./helper/*.ts": {
      "import": "./esm/helper/*.js",
      "require": "./script/helper/*.js"
    }
  },
  "bin": {
    "dweb-browser-devtools": "./bundle/index.js"
  },
  "darwinDarkModeSupport": true,
  "protocols": [
    {
      "name": "Dweb Browser",
      "schemes": [
        "dweb"
      ]
    }
  ],
  "build": {
    "appId": "com.instinct.macApp1st",
    "productName": "Dweb Browser",
    "artifactName": "$\{productName}-$\{version}-$\{arch}.$\{ext}",
    "asar": true,
    "files": [
      "assets",
      "bundle",
      "!node_modules"
    ],
    "directories": {
      "output": "../build"
    },
    "extraResources": [
      {
        "from": "./icons",
        "to": "./icons"
      }
    ],
    "mac": {
      "icon": "./icons/mac/icon.icns",
      "category": "public.app-category.developer-tools",
      "target": {
        "target": "default",
        "arch": [
          "x64",
          "arm64"
        ]
      },
      "provisioningProfile": "scripts/macApp1st_prov.provisionprofile"
    },
    "win": {
      "icon": "./icons/win/icon.ico",
      "target": {
        "target": "portable",
        "arch": [
          "x64",
          "arm64"
        ]
      },
      "publisherName": "Bnqkl Dweb Team"
    },
    "linux": {
      "icon": "./icons/mac/icon.icns",
      "category": "Development;Network",
      "maintainer": "Bnqkl Dweb Team",
      "mimeTypes": [
        "x-scheme-handler/dweb"
      ],
      "desktop": {
        "StartupNotify": "false",
        "StartupWMClass": "dweb",
        "Encoding": "UTF-8",
        "MimeType": "x-scheme-handler/dweb"
      },
      "target": [
        {
          "target": "deb",
          "arch": [
            "x64",
            "arm64"
          ]
        },
        {
          "target": "tar.xz",
          "arch": [
            "x64",
            "arm64"
          ]
        }
      ]
    }
  },
  "dependencies": {
  }
}
"""