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
    return env[key] ?: System.getProperty(key, "")
  }
}