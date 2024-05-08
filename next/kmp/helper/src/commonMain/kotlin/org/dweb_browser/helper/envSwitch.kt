package org.dweb_browser.helper

expect class EnvSwitch internal constructor(){
  fun add(switch: String)
  fun has(switch: String): Boolean
}

val envSwitch = EnvSwitch()