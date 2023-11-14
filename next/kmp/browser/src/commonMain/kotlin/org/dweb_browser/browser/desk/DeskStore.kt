package org.dweb_browser.browser.desk

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.createStore


class TaskbarStore(mm: MicroModule) {
  private val store = mm.createStore("taskbar", false)// createStore("taskbar/apps", false)

  suspend fun getApps(): MutableList<String> {
    return store.getOrPut("apps") {
      return@getOrPut mutableListOf()
    }
  }

  suspend fun setApps(data: MutableList<String>) {
    return store.set("apps", data)
  }
}