package org.dweb_browser.helper

actual class EnvSwitch {
  private val switchSet = mutableMapOf<String, String>()
  actual fun add(switch: String, value: String) {
    switchSet[switch] = value
  }

  actual fun isEnabled(switch: String): Boolean {
    return get(switch).isNotEmpty()
  }

  actual fun get(switch: String): String {
    return switchSet[switch] ?: ""
  }
}