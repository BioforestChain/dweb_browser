package org.dweb_browser.sys.configure

import org.dweb_browser.core.std.file.ext.createStore

class ConfigStore(configNMM: ConfigNMM.ConfigRuntime) {
  private val store = configNMM.createStore("configure", false)

  suspend fun get(key: String) = store.getOrNull<String>(key)

  suspend fun set(key: String, data: String) {
    return store.set(key, data)
  }
}