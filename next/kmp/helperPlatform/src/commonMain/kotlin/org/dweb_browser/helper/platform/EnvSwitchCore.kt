package org.dweb_browser.helper.platform

expect open class EnvSwitchCore() {
  fun isEnabled(switch: String): Boolean
  fun get(switch: String): String
  fun set(switch: String, value: String = "true")
  fun remove(switch: String)
}