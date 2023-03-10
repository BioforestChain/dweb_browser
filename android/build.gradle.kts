// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    apply(from = "versions.gradle")
    repositories {
        google()
        mavenCentral()
        maven(
            "https://maven.pkg.jetbrains.space/public/p/ktor/eap"
        )
        maven("https://jitpack.io")
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.4.2")
        val kotlin_version = "1.8.10"
        classpath(kotlin("gradle-plugin", version = kotlin_version))
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.19")
    }

}


tasks.register<Delete>("clean").configure {
    delete(rootProject.buildDir)
}

//
