package org.dweb_browser.helper

expect class EnvSwitch internal constructor() {
  fun add(switch: String, value: String = "true")
  fun isEnabled(switch: String): Boolean
  fun get(switch: String): String
}

val envSwitch = EnvSwitch()