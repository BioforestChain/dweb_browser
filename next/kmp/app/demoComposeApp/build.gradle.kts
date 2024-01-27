import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    id("target-common")
    id("target-js")
    id("org.jetbrains.compose")
}

kotlin {
    kmpBrowserJsTarget(project) {
        js{
            browser{
                commonWebpackConfig {
                    devServer = (devServer?: KotlinWebpackConfig.DevServer()).apply {
                        open = false
                    }
                    cssSupport {
                        enabled.set(true)
                    }
                }
                // 分发到electronApp作为静态资源使用
                distribution {
                    outputDirectory = File("${rootProject.rootDir}/app/electronApp/src/jsMain/resources/demo/compose/app")
                }
            }
            binaries.executable()
        }
        dependencies {
            implementation(project(":jsFrontend"))
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.material)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.components.resources)
        }
    }
}

compose.experimental {
    web.application {}
}



// Electron tasks

tasks.withType<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask>()
    .configureEach {

    }