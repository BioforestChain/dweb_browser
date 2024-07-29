package org.dweb_browser.helper.platform

import org.dweb_browser.helper.CommonBuildConfig

actual open class EnvSwitchCore : EnvSwitchWatcher() {
  private val switchStore = KeyValueStore("env")
  actual fun isEnabled(switch: String): Boolean {
    return get(switch) == "true"
  }

  actual fun get(switch: String): String {
    val key = "dweb-$switch"
    return switchStore.getString(key) ?: CommonBuildConfig.switchMaps[key] ?: ""
  }

  actual fun set(switch: String, value: String) {
    switchStore.setString("dweb-$switch", value)
    emitChanged(switch)
  }

  actual fun remove(switch: String) {
    switchStore.removeKeys("dweb-$switch")
    emitChanged(switch)
  }
}