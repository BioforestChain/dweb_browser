package org.dweb_browser.helper

actual open class EnvSwitchCore {
  actual fun add(switch: String, value: String) {
    System.setProperty("dweb-$switch", value)
  }

  private val env = System.getenv()

  actual fun isEnabled(switch: String): Boolean {
    return get(switch) == "true"
  }

  actual fun get(switch: String): String {
    val key = "dweb-$switch"
    // 环境变量不方便配置 “-” 符号，所以替换成下划线
    return env[key] ?: env[key.replace("-", "_")] ?: System.getProperty(key, "")
  }
}