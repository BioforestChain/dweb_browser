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
        classpath("com.android.tools.build:gradle:7.2.2")
        val kotlinVersion by System.getProperties()
        classpath(kotlin("gradle-plugin", version = "${kotlinVersion}"))
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.19")
    }

}


tasks.register<Delete>("clean").configure {
    delete(rootProject.buildDir)
}

//
