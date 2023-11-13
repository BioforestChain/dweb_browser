package org.dweb_browser.sys.configure

import org.dweb_browser.core.std.file.ext.createStore

class ConfigStore(configNMM: ConfigNMM) {
  private val store = configNMM.createStore("configure", false)

  suspend fun get(key: String): String {
    return store.get(key)
  }

  suspend fun set(key: String, data: String) {
    return store.set(key, data)
  }
}