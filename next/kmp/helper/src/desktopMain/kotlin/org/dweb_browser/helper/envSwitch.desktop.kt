package org.dweb_browser.helper

actual class EnvSwitch {
  actual fun add(switch: String) {
    System.setProperty("dweb-$switch", "true")
  }

  private val env = System.getenv()

  actual fun has(switch: String): Boolean {
    val key = "dweb-$switch"
    return (env[key] ?: System.getProperty(key, "false")) == "true"
  }
}