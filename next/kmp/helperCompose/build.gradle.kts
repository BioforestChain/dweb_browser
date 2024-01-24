plugins {
  id("kmp-compose")
}

kotlin {
  kmpComposeTarget(project){
    dependencies{
      implementation(projects.helper)
      implementation(projects.helperPlatform)
    }
  }
  kmpAndroidTarget(project)
  kmpIosTarget(project)
  kmpBrowserJsTarget(project)
}
