import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    id("target-common")
    id("target-js")
}

beforeEvaluate {
    configureYarn()
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
                    outputDirectory = File("${rootProject.rootDir}/app/electronApp/src/jsMain/resources/demoReactApp")
                }
            }
            // 测试用
            binaries.executable()
        }
        dependencies {
            implementation(project(":jsFrontend"))
            implementation("org.jetbrains.kotlin-wrappers:kotlin-react:18.2.0-pre.687")
            implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom:18.2.0-pre.687")
//      implementation("org.jetbrains.kotlin-wrappers:kotlin-react-redux:7.2.6-pre.687")
//      implementation("org.jetbrains.kotlin-wrappers:kotlin-react-router-dom:6.20.1-pre.687")

//        implementation(enforcedPlatform("org.jetbrains.kotlin-wrappers:kotlin-wrappers-bom:1.0.0-pre.680"))
//        implementation("org.jetbrains.kotlin-wrappers:kotlin-react")
//        implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom")

            //Kotlin React Emotion (CSS) (chapter 3)
//        implementation("org.jetbrains.kotlin-wrappers:kotlin-emotion:18.2.0-pre.687")
        }
    }
}


tasks.register("stage") {
    dependsOn("build")
}


// Electron tasks

tasks.withType<org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask>()
    .configureEach {

    }