package info.bagen.dwebbrowser.microService.browser.jmm

import info.bagen.dwebbrowser.App
import org.dweb_browser.browserUI.util.getString
import org.dweb_browser.browserUI.util.saveString
import org.dweb_browser.microservice.help.types.MMID


object JmmStore {

  const val JMM_APPS = "JMM/apps"

  fun getMetadataUrl(key: MMID): String {
    return App.appContext.getString(key)
  }

  fun setMetadataUrl(key: MMID, data: String) {
    return App.appContext.saveString(key, data)
  }
}