package org.dweb_browser.helper

actual class EnvSwitch {
  private val switchSet = mutableSetOf<String>()
  actual fun add(switch: String) {
    switchSet.add(switch)
  }

  actual fun has(switch: String): Boolean {
    return switchSet.contains(switch)
  }
}