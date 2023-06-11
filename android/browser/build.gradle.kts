plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("com.google.devtools.ksp")
}

android {
  namespace = "org.dweb_browser.browser"
  compileSdk = 33

  defaultConfig {
    applicationId = "org.dweb_browser.browser"
    minSdk = 24
    targetSdk = 33
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables {
      useSupportLibrary = true
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  kotlinOptions {
    jvmTarget = "1.8"
  }
  buildFeatures {
    compose = true
  }
  composeOptions {
    kotlinCompilerExtensionVersion = "1.4.3"
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

dependencies {

  implementation("androidx.core:core-ktx:1.10.1")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
  implementation("androidx.activity:activity-compose:1.7.2")
  implementation("androidx.compose:compose-bom:2023.05.01")
  androidTestImplementation("androidx.compose:compose-bom:2023.05.01")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-graphics")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.compose.material3:material3:1.2.0-alpha02")
  implementation(project(mapOf("path" to ":helper")))
  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test.ext:junit:1.1.5")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
  androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00"))
  androidTestImplementation("androidx.compose.ui:ui-test-junit4")
  debugImplementation("androidx.compose.ui:ui-tooling")
  debugImplementation("androidx.compose.ui:ui-test-manifest")
  implementation("androidx.compose.material:material-icons-extended")


  // 增加 room 存储列表数据
  val roomVersion = "2.5.1"
  implementation("androidx.room:room-runtime:$roomVersion")
  ksp("androidx.room:room-compiler:$roomVersion")
  //annotationProcessor("androidx.room:room-compiler:$roomVersion") // 注释处理工具
  implementation("androidx.room:room-ktx:$roomVersion") // kotlin扩展和协同程序对Room的支持

  // 加载图片 coil
  implementation("io.coil-kt:coil:2.4.0")
  implementation("io.coil-kt:coil-compose:2.4.0")
  implementation("io.coil-kt:coil-svg:2.4.0")
  implementation("io.coil-kt:coil-video:2.4.0")
  implementation("io.coil-kt:coil-gif:2.4.0")
}