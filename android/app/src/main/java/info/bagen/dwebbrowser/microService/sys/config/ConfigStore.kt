package info.bagen.dwebbrowser.microService.sys.config

import info.bagen.dwebbrowser.App
import org.dweb_browser.browserUI.util.getString
import org.dweb_browser.browserUI.util.saveString

object ConfigStore {
  const val Config = "config"
  fun get(key: String): String {
    return App.appContext.getString(key,"")
  }

  fun set(key: String, data: String) {
    return App.appContext.saveString(key, data)
  }
}