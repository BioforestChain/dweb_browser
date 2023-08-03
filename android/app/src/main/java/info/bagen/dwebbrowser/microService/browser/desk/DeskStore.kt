package info.bagen.dwebbrowser.microService.browser.desk

import info.bagen.dwebbrowser.App
import org.dweb_browser.browserUI.util.getList
import org.dweb_browser.browserUI.util.saveList


object DeskStore {

  const val TASKBAR_APPS = "taskbar/apps"

  fun get(key: String): MutableList<String> {
    return App.appContext.getList(key) ?: mutableListOf()
  }

  fun set(key: String, data: MutableList<String>) {
    return App.appContext.saveList(key, data)
  }
}