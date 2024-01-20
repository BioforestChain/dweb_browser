plugins {
  id("kmp-compose")
}

kotlin {
  kmpComposeTarget(project){
    dependencies{
      implementation(projects.helper)
      implementation(projects.helperPlatform)
      implementation(projects.pureIO)
    }
  }
  kmpAndroidTarget(project)
  kmpIosTarget(project)
}
