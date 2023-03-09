plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    android {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "kmmSharedModule"
        }
    }

    sourceSets {

        val commonMain by getting {
            dependencies {
                /// io库
                implementation("org.jetbrains.kotlinx:kotlinx-io:0.1.16")
                implementation("com.squareup.okio:okio:3.3.0")
                /// 协程库
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
                /// 序列化反序列化库
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
                /// 原子库
                implementation("org.jetbrains.kotlinx:atomicfu:0.20.0")
                /// time/date 库
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
                /// base64库
                implementation("com.diglol.encoding:encoding:0.2.0")
            }
        }
//        val commonTest by getting {
//            dependencies {
//                implementation(kotlin("test"))
//            }
//        }
        val androidMain by getting
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependencies {
            }
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
//        val iosX64Test by getting
//        val iosArm64Test by getting
//        val iosSimulatorArm64Test by getting
//        val iosTest by creating {
//            dependsOn(commonTest)
//            iosX64Test.dependsOn(this)
//            iosArm64Test.dependsOn(this)
//            iosSimulatorArm64Test.dependsOn(this)
//        }
    }
}

android {
    namespace = "info.bagen.kmmsharedmodule"
    compileSdk = 33
    defaultConfig {
        minSdk = 24
        targetSdk = 33
    }
}
