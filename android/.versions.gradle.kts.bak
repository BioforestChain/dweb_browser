//App
class AppVersion {
    companion object {
        val versionCode = 4
        val versionName = "1.0.3"
    }
}

//build version
class BuildVersion {
    companion object {
        minSdk = 28
        targetSdk = 33
        compileSdk = 33
        buildTools = "30.0.3"
    }
}

class DepsVersion {
    companion object {
        //AndroidX
        private val material = "1.2.0"
        private val appcompat = "1.1.0"
        private val constraintLayout = "2.0.4"

        //test
        private val junit = "1.1.2"
        private val test = "1.2.0"
        private val runner = "1.2.0"
        private val espresso = "3.3.0"

        private val bintrayPublish = "1.0.0"
        private val mavenPublish = "0.13.0"

        private val gralde = "4.1.3"
        private val kotlin = "1.6.20"
        private val coreKtx = "1.3.2"

        private val camerax = "1.0.2"

        private val easypermissions = "3.0.0"

        class AndroidX {
            val design = "com.google.android.material:material:${material}"
            val appcompat = "androidx.appcompat:appcompat:${appcompat}"
            val constraintlayout = "androidx.constraintlayout:constraintlayout:${constraintLayout}"
            val corektx = "androidx.core:core-ktx:$coreKtx"
        }

        class Test {
            val junit = "androidx.test.ext:junit:$junit"
            val test = "androidx.test:core:$test"
            val runner = "androidx.test:runner:$runner"
            val espresso = "androidx.test.espresso:espresso-core:$espresso"
        }

        class Kotlin {
            val stdlib = "org.jetbrains.kotlin:kotlin-stdlib:$kotlin"
        }

        class CameraX {
            val camera_core = "androidx.camera:camera-core:$camerax"
            val camera_camera2 = "androidx.camera:camera-camera2:$camerax"
            val camera_lifecycle = "androidx.camera:camera-lifecycle:$camerax"
            val camera_view = "androidx.camera:camera-view:1.0.0-alpha25"

            val google_mlkit_barcode_scanning = "com.google.mlkit:barcode-scanning:17.0.0"

            val app_dialog = "com.github.jenly1314.AppUpdater:app-dialog:1.1.0"
        }
    }
}
