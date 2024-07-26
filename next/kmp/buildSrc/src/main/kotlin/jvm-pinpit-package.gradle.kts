import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val libs = the<LibrariesForLibs>()

plugins {
  kotlin("jvm")
  id("de.mobanisto.pinpit")
}

val attributeUsage = Attribute.of("org.gradle.usage", String::class.java)

val currentOs: Configuration by configurations.creating {
  extendsFrom(configurations.implementation.get())
  attributes { attribute(attributeUsage, "java-runtime") }
}

val macosX64: Configuration by configurations.creating {
  extendsFrom(configurations.implementation.get())
  attributes { attribute(attributeUsage, "java-runtime") }
}

val macosArm64: Configuration by configurations.creating {
  extendsFrom(configurations.implementation.get())
  attributes { attribute(attributeUsage, "java-runtime") }
}

sourceSets {
  main {
    java {
      compileClasspath = currentOs
      runtimeClasspath = currentOs
    }
  }
}

java {
  sourceCompatibility = JavaVersion.valueOf(libs.versions.javaVersion.get())
  targetCompatibility = JavaVersion.valueOf(libs.versions.javaVersion.get())
}

tasks.withType<KotlinCompile> {
  compilerOptions {
    jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmTarget.get()))
  }
}

//// 必须添加仓库，否则会导致一些依赖获取失败
//repositories {
//  google {
//    mavenContent {
//      includeGroupByRegex(".*google.*")
//      includeGroupByRegex(".*android.*")
//    }
//  }
//  mavenCentral()
//  maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
//  maven("https://androidx.dev/storage/compose-compiler/repository")
//
//  /**
//   * 北美 的仓库地址
//   * https://github.com/TeamDev-IP/JxBrowser-Gradle-Plugin/blob/main/src/main/kotlin/com/teamdev/jxbrowser/gradle/Repository.kt
//   */
//  maven("https://us-maven.pkg.dev/jxbrowser/releases")
//  maven("https://europe-maven.pkg.dev/jxbrowser/eaps")
//}
//
//dependencies {
//  currentOs(compose.desktop.currentOs)
//  macosX64(compose.desktop.macos_x64)
//  macosArm64(compose.desktop.macos_arm64)
//  implementation(projects.platformDesktop)
//  implementation(projects.shared) // 用于导入 resources 文件夹
//  implementation(projects.helper)
//  implementation(projects.helperCompose)
//  implementation(projects.helperPlatform)
//  implementation(projects.pureIO)
//  implementation(projects.pureHttp)
//  implementation(projects.pureImage)
//  implementation(projects.core)
//  implementation(projects.window)
//  implementation(projects.sys)
//  implementation(projects.browser)
//  implementation(projects.dwebview)
//  implementation(libs.ktor.server.core)
//  implementation(libs.ktor.server.netty)
//}

