plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}
android {
    compileSdk = 33
    defaultConfig {
        applicationId = "info.bagen.dwebbrowser"
        minSdk = 28
        targetSdk = 33
        versionCode = 3
        versionName = "1.1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        //ndk.abiFilters.addAll(listOf("armeabi-v7a", "x86", "x86_64"))
        //ndk.abiFilters = listOf("arm64-v8a")
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    sourceSets {
        named("main") {
            jniLibs.setSrcDirs(listOf("src/main/libs"))
            assets.setSrcDirs(listOf("src/main/assets"))
            // Add generated code folder to app module source set
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true //开启代码混淆
            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            )
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")//移除无用的resource文件
        }
        create("sit") {
            isDebuggable = true
        }
        create("benchmark") {
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            isDebuggable = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        val kotlin_version = "1.8.10"
        freeCompilerArgs += listOf(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=$kotlin_version"
        )
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }
    lint {
        abortOnError = false
        warning += listOf("InvalidPackage")
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // 添加 http4k 框架后，会有异常报错，需要添加如下内容
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/io.netty.versions.properties"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
    namespace = "info.bagen.dwebbrowser"
}


dependencies {

//    val ktor_version = "2.2.3"
    /// 网络开发相关
    implementation(platform("org.http4k:http4k-bom:4.39.0.0"))
    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-multipart")
    implementation("org.http4k:http4k-client-apache")
    implementation("org.http4k:http4k-client-okhttp")
    implementation("org.http4k:http4k-server-ktorcio")
//    implementation("org.http4k:http4k-format-jackson") // payload to json
//    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("org.jsoup:jsoup:1.15.3")

    // Android 相关
//    implementation("androidx.work:work-runtime:2.7.1")
//    implementation("androidx.work:work-runtime-ktx:2.7.1")
//    implementation("androidx.work:work-rxjava2:2.7.1")
//    implementation("androidx.work:work-gcm:2.7.1")
//    implementation("androidx.work:work-multiprocess:2.7.1")
//    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.3.1")
    implementation("androidx.core:core-splashscreen:1.0.0-beta02")
//    implementation("androidx.navigation:navigation-fragment-ktx:2.5.1")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.1")
    implementation("androidx.gridlayout:gridlayout:1.0.0")
    val accompanistVersion = "0.31.2-alpha" // "0.29.2-rc"
    implementation("com.google.accompanist:accompanist-webview:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-navigation-material:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-navigation-animation:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-systemuicontroller:$accompanistVersion")
    implementation("com.google.accompanist:accompanist-permissions:$accompanistVersion") // 权限功能
    implementation("com.google.accompanist:accompanist-insets:$accompanistVersion") // 界面安全区功能
    implementation("com.google.accompanist:accompanist-insets-ui:$accompanistVersion") // 界面安全区功能

    implementation("androidx.profileinstaller:profileinstaller:1.3.0")
    testImplementation("org.junit.jupiter:junit-jupiter")
    val appcompatVersion = "1.6.1"
    implementation("androidx.appcompat:appcompat:$appcompatVersion")
    implementation("androidx.appcompat:appcompat-resources:$appcompatVersion")
//    val constraintlayoutVersion = "2.1.4"
//    implementation("androidx.constraintlayout:constraintlayout:$constraintlayoutVersion")
    val coreVersion = "1.9.0"
    implementation("androidx.core:core-ktx:$coreVersion")
    implementation("androidx.datastore:datastore:1.0.0")
    implementation("androidx.datastore:datastore-preferences:1.1.0-alpha01")
    val activity_version = "1.7.0-beta02"
    // Java language implementation
    implementation("androidx.activity:activity:$activity_version")
    // Kotlin
    implementation("androidx.activity:activity-ktx:$activity_version")
    implementation("androidx.activity:activity-compose:$activity_version")

    /// Compose 相关
    val composeBom = platform("androidx.compose:compose-bom:2023.03.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material3:material3:1.1.0-rc01")
    implementation("androidx.compose.material3:material3-window-size-class:1.1.0-rc01")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("com.google.android.material:material")

    // 工具库
    implementation(kotlin("stdlib"))
    implementation("com.google.code.gson:gson:2.10.1")
//    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.2")
    implementation("com.daveanthonythomas.moshipack:moshipack:1.0.1") // message-pack

    //扫码核心库
    // implementation(project(":mlkit-camera-core"))
    // implementation(project(":mlkit-barcode-scanning"))
    // implementation("com.github.jenly1314.AppUpdater:app-dialog:1.1.0")
    implementation("com.google.mlkit:barcode-scanning:17.1.0")
    val cameraVersion = "1.3.0-alpha06"
    implementation("androidx.camera:camera-core:$cameraVersion")
    implementation("androidx.camera:camera-camera2:$cameraVersion")
    implementation("androidx.camera:camera-view:$cameraVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraVersion")
    // implementation("com.google.accompanist:accompanist-permissions:0.31.2-alpha")

    /// 测试相关
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.4.0")

    testImplementation(platform("org.junit:junit-bom:5.9.2"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher") {
        because("Only needed to run tests in a version of IntelliJ IDEA that bundles older versions")
    }
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")

    // exoplayer
    //implementation 'com.google.android.exoplayer:exoplayer:2.14.1'
    implementation("com.google.android.exoplayer:exoplayer-core:2.18.4")
    implementation("com.google.android.exoplayer:exoplayer-ui:2.18.4")
    // 由于引入exoplayer后导致com.google.guava:listenablefuture not found -> https://github.com/google/ExoPlayer/issues/7993
    implementation("com.google.guava:guava:29.0-android")

    // 类似ViewPager功能 改为 androidx.compose.foundation.pager
    //implementation("com.google.accompanist:accompanist-pager:0.27.0")
    //implementation("com.google.accompanist:accompanist-pager-indicators:0.27.0")

    // 解压文件
    implementation("org.apache.commons:commons-compress:1.21")

    // 加载图片 coil
    implementation("io.coil-kt:coil:2.2.2")
    implementation("io.coil-kt:coil-compose:2.2.2")
    implementation("io.coil-kt:coil-svg:2.2.2")
    implementation("io.coil-kt:coil-video:2.2.2")
    implementation("io.coil-kt:coil-gif:2.2.2")
//    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1") // 因为导入coil后，编译失败，duplicate
    // 生物识别
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    /// 依赖
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // 增加 room 存储列表数据
    val roomVersion = "2.5.0"
    implementation("androidx.room:room-runtime:$roomVersion")
    kapt("androidx.room:room-compiler:2.5.0") // To use Kotlin annotation processing tool (kapt)
    //annotationProcessor("androidx.room:room-compiler:$roomVersion") // 注释处理工具
    implementation("androidx.room:room-ktx:$roomVersion") // kotlin扩展和协同程序对Room的支持
}

tasks.withType<Test> {
    useJUnitPlatform()

}

//val rustBasePath = "../../rust_lib"
//val archTriplets = mutableMapOf<String, String>()
////listOf(
//////        "arm64-v8a" = "aarch64-linux-android",
//////        "armeabi-v7a" = "armv7-linux-androideabi",
////)
//
//archTriplets.each { (arch, target) ->
//    project.extra["cargo_target_directory"] = rustBasePath + "/target"
//    // Build with cargo
//    tasks.create(
//        name = "cargo-build-${arch}",
//        type: Exec,
//        description = "Building core for ${arch}"
//    ) {
//        workingDir = rustBasePath
//        commandLine = listOf("cargo", "build", "--target=${target}", "--release")
//        environment = listOf(
//            "RUSTY_V8_ARCHIVE",
//            file(rustBasePath + "/assets/rusty_v8_mirror/v0.48.1/librusty_v8_release_aarch64-linux-android.a")
//        )
//    }
//    // Sync shared native dependencies
//    tasks.create(name = "sync-rust-deps-${arch}", type: Sync, dependsOn = "cargo-build-${arch}") {
//        from = "${rustBasePath}/src/libs/${arch}"
//        include = "*.so"
//        into = "src/main/libs/${arch}"
//    }
//    // Copy build libs into this app"s libs directory
//    tasks.create(
//        name = "rust-deploy-${arch}",
//        type: Copy,
//        dependsOn = "sync-rust-deps-${arch}",
//        description = "Copy rust libs for (${arch}) to jniLibs"
//    ) {
//        from = "${project.ext.cargo_target_directory}/${target}/release"
//        include = "*.so"
//        into = "src/main/libs/${arch}"
//    }
//
//    // Hook up tasks to execute before building java
//    tasks.withType(JavaCompile) {
//        implementation(Task -> implementationTask.dependsOn "rust-deploy-${arch}")
//    }
//    preBuild.dependsOn "rust-deploy-${arch}"
//
//    // Hook up clean tasks
//    tasks.create(
//        name = "clean-${arch}",
//        type: Delete,
//        description = "Deleting built libs for ${arch}",
//        dependsOn = "cargo-output-dir-${arch}"
//    ) {
//        delete fileTree ("${project.ext.cargo_target_directory}/${target}/release") {
//            include("*.so")
//        }
//    }
//    clean.dependsOn "clean-${arch}"
//}
