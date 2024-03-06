plugins {
//  alias(libs.plugins.kotlinMultiplatform)
//  alias(libs.plugins.androidLibrary)
    id("kmp-library")
}

kotlin {
    kmpBrowserJsTarget(project) {
        dependencies {
            api(libs.kotlinx.html)
            api(libs.kotlin.js)
            api(libs.kotlin.web)
            api(libs.kotlin.browser)
        }
    }
}
