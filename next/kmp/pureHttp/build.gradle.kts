plugins {
  id("kmp-library")
}

kotlin {
  kmpCommonTarget(project) {
    dependencies {
      api(libs.ktor.io)
      api(libs.ktor.http)
      api(libs.ktor.client.encoding)

      implementation(projects.helper)
      implementation(projects.helperPlatform)
    }
    @Suppress("OPT_IN_USAGE")
    applyHierarchyTemplate {
      common {
        group("ktor") {
          withAndroidTarget()
          withIosTarget()
          withDesktopTarget()
        }
      }
    }
  }

  /**
   * 这里server的部分单独抽出来，不和client放在一起，是因为js-target是不支持server的
   */
  val ktorMain by sourceSets.creating {
    dependencies {
      implementation(libs.ktor.server.host.common)
      implementation(libs.ktor.server.core)
      implementation(libs.ktor.server.websockets)
    }
  }
  kmpAndroidTarget(project) {
    dependencies {
      implementation(libs.ktor.server.cio)
      implementation(libs.ktor.client.okhttp)
    }
  }
  kmpIosTarget(project) {
    dependencies {
      implementation(libs.ktor.server.cio)
      implementation(libs.ktor.client.darwin)
    }
  }
  kmpDesktopTarget(project) {
    dependencies {
      // 兼容性最好、功能性最全
      implementation(libs.ktor.client.okhttp)
      // 异步轻量，性价比最高
      implementation(libs.ktor.server.netty)
      implementation(libs.ktor.server.jetty)
      implementation(libs.ktor.network.tls.certificates)
    }
  }
}
