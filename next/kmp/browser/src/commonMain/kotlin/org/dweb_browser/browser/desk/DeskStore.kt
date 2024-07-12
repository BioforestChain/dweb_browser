package org.dweb_browser.browser.desk

import org.dweb_browser.browser.web.data.WebLinkManifest
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.createStore

class TaskbarStore(mm: MicroModule.Runtime) {
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

class DeskSortStore(mm: MicroModule.Runtime) {
  private val store = mm.createStore("desk_sort", false)// createStore("taskbar/apps", false)

  suspend fun getApps(): MutableList<String> {
    val sortedList = store.getAll<Int>().entries.sortedBy { it.value }
    return sortedList.map { it.key }.toMutableList()
  }

  suspend fun has(mmid: MMID): Boolean {
    return store.getAll<Int>().containsKey(mmid)
  }

  suspend fun push(mmid: MMID) {
    store.set(mmid, store.getAll<Int>().size + 1)
  }

  suspend fun delete(mmid: MMID) {
    store.delete(mmid)
  }
}

class WebLinkStore(mm: MicroModule.Runtime) {
  private val store = mm.createStore("web_link", false)

  suspend fun getOrPut(key: MMID, value: WebLinkManifest): WebLinkManifest {
    return store.getOrPut(key) { value }
  }

  suspend fun get(key: MMID): WebLinkManifest? {
    return store.getOrNull(key)
  }

  suspend fun getAll(): MutableMap<MMID, WebLinkManifest> {
    return store.getAll()
  }

  suspend fun set(key: MMID, value: WebLinkManifest) {
    store.set(key, value)
  }

  suspend fun delete(key: MMID): Boolean {
    return store.delete(key)
  }
}