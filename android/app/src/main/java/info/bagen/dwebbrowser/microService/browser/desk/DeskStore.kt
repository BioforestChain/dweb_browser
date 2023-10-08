package info.bagen.dwebbrowser.microService.browser.desk

import org.dweb_browser.microservice.core.MicroModule
import org.dweb_browser.microservice.std.file.ext.createStore


class DeskStore(mm: MicroModule) {
  private val store = mm.createStore("desk", false)// createStore("taskbar/apps", false)

  suspend fun getTaskbarApps(): MutableList<String> {
    return store.get("taskbar/apps")
  }

  suspend fun setTaskbarApps(data: MutableList<String>) {
    return store.set("taskbar/apps", data)
  }
}