package org.dweb_browser.browser.desk

import org.dweb_browser.core.help.types.MMID
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

class DaskSortStore(mm: MicroModule) {
  private val store = mm.createStore("deskSort", false)// createStore("taskbar/apps", false)

  suspend fun getApps(): MutableList<String> {
    return store.getOrPut("appSort") {
      return@getOrPut mutableListOf()
    }
  }

  suspend fun push(mmid: MMID) {
    val list = getApps()
    list.add(mmid)
    store.set("appSort", list)
  }

  suspend fun delete(mmid: MMID) {
    val list = getApps()
    list.remove(mmid)
    store.set("appSort", list)
  }
}