plugins {
  id("java-gradle-plugin")
  id("org.jetbrains.kotlin.jvm") version "1.9.0-Beta"
}

repositories {
  google()
  mavenCentral()
  gradlePluginPortal()
}

java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
  //添加Gradle相关的API，否则无法自定义Plugin和Task
  implementation(gradleApi())
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0-Beta")
}

gradlePlugin {
  plugins {
    create("com.version.manager") {
      //添加插件
      id = "com.version.manager"
      //在根目录创建类 VersionPlugin 继承 Plugin<Project>
      implementationClass = "com.version.manager.VersionPlugin"
    }
  }
}