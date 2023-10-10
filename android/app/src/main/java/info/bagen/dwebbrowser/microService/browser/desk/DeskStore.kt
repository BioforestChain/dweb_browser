package info.bagen.dwebbrowser.microService.browser.desk

import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.createStore


class DeskStore(mm: MicroModule) {
  private val store = mm.createStore("desk", false)// createStore("taskbar/apps", false)

  suspend fun getTaskbarApps(): MutableList<String> {
    return store.getOrPut("taskbar/apps") {
     return@getOrPut mutableListOf()
    }
  }

  suspend fun setTaskbarApps(data: MutableList<String>) {
    return store.set<MutableList<String>>("taskbar/apps", data)
  }
}