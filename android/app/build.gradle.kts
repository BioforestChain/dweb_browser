import com.google.protobuf.gradle.*

//@file:("../version.gradle.kts")

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.protobuf")//.version("0.8.19")// dependence datastore protobuf 20230306
}
android {
    compileSdk = 33
    defaultConfig {
        applicationId = "info.bagen.rust.plaoc"
        minSdk = 28
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk.abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
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
            isMinifyEnabled = false
            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
            )
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += listOf(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=true"
        )
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.0"
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
}

val ktorVersion by System.getProperties()

dependencies {
    /// 网络开发相关
    implementation(platform("org.http4k:http4k-bom:4.39.0.0"))
    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-client-apache")
    implementation("org.http4k:http4k-client-okhttp")
    implementation("org.http4k:http4k-server-ktorcio")
    implementation("org.http4k:http4k-format-jackson") // payload to json
    implementation("io.ktor:ktor-client-core:$ktorVersion")


    // Android 相关
    implementation("androidx.work:work-runtime:2.7.1")
    implementation("androidx.work:work-runtime-ktx:2.7.1")
    implementation("androidx.work:work-rxjava2:2.7.1")
    implementation("androidx.work:work-gcm:2.7.1")
    implementation("androidx.work:work-multiprocess:2.7.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.3.1")
    implementation("androidx.core:core-splashscreen:1.0.0-beta02")
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.appcompat:appcompat:1.5.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.1")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.1")
    implementation("androidx.gridlayout:gridlayout:1.0.0")
    implementation("com.google.accompanist:accompanist-webview:0.24.7-alpha")
    implementation("com.google.accompanist:accompanist-navigation-material:0.24.7-alpha")
    implementation("com.google.accompanist:accompanist-navigation-animation:0.24.7-alpha")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.24.7-alpha")
    implementation("androidx.profileinstaller:profileinstaller:1.2.0-alpha02")
    testImplementation("org.junit.jupiter:junit-jupiter")
    val appcompat_version = "1.6.1"
    implementation("androidx.appcompat:appcompat:$appcompat_version")
    implementation("androidx.appcompat:appcompat-resources:$appcompat_version")
    val constraintlayout_version = "2.1.4"
    implementation("androidx.constraintlayout:constraintlayout:$constraintlayout_version")
    val core_version = "1.9.0"
    implementation("androidx.core:core-ktx:$core_version")
    implementation("androidx.datastore:datastore:1.0.0") // dependence datastore protobuf 20230306
    //implementation("androidx.datastore:datastore-preferences:1.1.0-alpha01") // dependence datastore protobuf 20230306
    //implementation("com.google.protobuf:protobuf-javalite:3.19.2") // dependence datastore protobuf 20230306
    implementation("com.google.protobuf:protobuf-java:3.19.2") // dependence datastore protobuf 20230306

    /// Compose 相关
    val composeBom = platform("androidx.compose:compose-bom:2023.01.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material:material")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.5.1")
    implementation("com.google.android.material:material")

    // 工具库
    implementation(kotlin("stdlib"))
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.2")
    implementation("com.daveanthonythomas.moshipack:moshipack:1.0.1") // message-pack


    //扫码核心库
    implementation(project(":mlkit-camera-core"))
    implementation(project(":mlkit-barcode-scanning"))
    implementation("com.github.jenly1314.AppUpdater:app-dialog:1.1.0")

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
    implementation("com.google.android.exoplayer:exoplayer-core:2.14.1")
    implementation("com.google.android.exoplayer:exoplayer-ui:2.14.1")
    // 由于引入exoplayer后导致com.google.guava:listenablefuture not found -> https://github.com/google/ExoPlayer/issues/7993
    implementation("com.google.guava:guava:29.0-android")

    // 类似ViewPager功能
    implementation("com.google.accompanist:accompanist-pager:0.27.0")
    implementation("com.google.accompanist:accompanist-pager-indicators:0.27.0")

    // 解压文件
    implementation("org.apache.commons:commons-compress:1.21")

    // 增加注入ViewModel // 将implementation改为api是为了让app主应用能够调用到
    implementation("io.insert-koin:koin-androidx-compose:3.2.1")
    implementation("io.insert-koin:koin-android:3.1.2")
    implementation("io.insert-koin:koin-android-compat:3.1.2")

    // 加载图片 coil
    implementation("io.coil-kt:coil:2.2.2")
    implementation("io.coil-kt:coil-svg:2.2.2")
    implementation("io.coil-kt:coil-compose:2.2.2")
    implementation("io.coil-kt:coil-video:2.2.2")
    implementation("io.coil-kt:coil-gif:2.2.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1") // 因为导入coil后，编译失败，duplicate

    /// 依赖
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
}

// dependence datastore protobuf 20230306
protobuf {
    val configuration = this
    configuration.protoc {
        artifact = "com.google.protobuf:protoc:3.19.2"
    }

    // Generates the java Protobuf-lite code for the Protobufs in this project. See
    // https://github.com/google/protobuf-gradle-plugin#customizing-protobuf-compilation
    // for more information.
    // configuration.generatedFilesBaseDir = project.layout.projectDirectory.dir("build/generated").toString()

    configuration.generateProtoTasks {

        this.all().forEach { task ->
            task.builtins {
                id("java") {
                    java { }
                }
            }
        }
    }
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
