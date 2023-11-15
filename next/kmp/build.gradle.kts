plugins {
  id(libs.plugins.androidApplication.get().pluginId) apply false
  id(libs.plugins.androidLibrary.get().pluginId) apply false
  id(libs.plugins.kotlinAndroid.get().pluginId) apply false
  id(libs.plugins.kotlinxMultiplatform.get().pluginId) apply false
  id(libs.plugins.kotlinJvm.get().pluginId) apply false
}

//tasks.register("clean", Delete::class) {
//  delete(rootProject.buildDir)
//}
