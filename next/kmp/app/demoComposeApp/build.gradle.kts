plugins {
    id("target-common")
    id("target-js")
    id("org.jetbrains.compose")
}

kotlin {
    kmpBrowserJsTarget(project) {
        js{
            binaries.executable()
        }
        dependencies {
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