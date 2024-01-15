plugins {
  id("target-common")
  id("target-js")
}

beforeEvaluate {
  configureYarn()
}

kotlin {
  kmpNodeJsTarget(project)
}

