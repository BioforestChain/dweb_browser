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