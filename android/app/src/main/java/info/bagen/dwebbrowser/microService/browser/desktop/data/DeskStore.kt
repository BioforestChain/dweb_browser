package info.bagen.dwebbrowser.microService.browser.desktop.data

import info.bagen.dwebbrowser.App
import org.dweb_browser.browserUI.util.getList
import org.dweb_browser.browserUI.util.saveList
import org.dweb_browser.microservice.help.MMID


object DeskStore {

  const val TASKBAR_APPS = "taskbar/apps"

  fun get(key: String): MutableList<String> {
    return App.appContext.getList(key) ?: mutableListOf()
  }

  fun set(key: String, data: List<String>) {
    return App.appContext.saveList(key, data)
  }
}