package org.dweb_browser.helper.platform

actual open class EnvSwitchCore : EnvSwitchWatcher() {
  private val env = System.getenv()

  actual fun isEnabled(switch: String): Boolean {
    return get(switch) == "true"
  }

  actual fun get(switch: String): String {
    val key = "dweb-$switch"
    // 环境变量不方便配置 “-” 符号，所以替换成下划线
    return env[key]
      ?: env[key.replace("-", "_")]
      ?: System.getProperty(key) ?: ""
  }

  actual fun set(switch: String, value: String) {
    System.setProperty("dweb-$switch", value)
    emitChanged(switch)
  }

  actual fun remove(switch: String) {
    System.clearProperty("dweb-$switch")
    emitChanged(switch)
  }
}