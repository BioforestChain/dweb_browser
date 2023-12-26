plugins {
  id("multiplatform-plugins")
  id("com.android.library")
}

kotlin {
  mobileTarget()
}

// 配置测试环境
configureAllTests()

// 配置 nodejs
rootProject.configureNodejs()