package org.dweb_browser.dwebview

import org.dweb_browser.helper.platform.webViewEngine
import org.dweb_browser.platform.desktop.webview.jxBrowserEngine

class ChromiumWebProfileStore private constructor() : DWebProfileStore {
  companion object {
    val instance by lazy { ChromiumWebProfileStore() }
  }

  fun getAllProfiles() = mutableMapOf<String, ChromiumWebProfile>().also { result ->
    jxBrowserEngine.allEngines.map { (enginePath, engine) ->
      println("QAQ enginePath=$enginePath")
      val engineName = webViewEngine.resolveDir(enginePath)
      for (profile in engine.profiles().list()) {
        if (profile.isIncognito || profile.isDefault) {
          continue
        }
        val profileName = profile.name()
        result[profileName] = ChromiumWebProfile(profile, profileName)
      }
    }
  }

  override suspend fun getAllProfileNames() = getAllProfiles().keys.toList()

  override suspend fun isUsingProfile(name: String): Boolean {
    return getAllProfiles()[name]?.profile?.browsers()?.isNotEmpty() ?: false
  }

  override suspend fun deleteProfile(name: String): Boolean {
    return getAllProfiles()[name]?.let { item ->
      item.engine.profiles().delete(item.profile)
      true
    } ?: false
  }
}

val chromiumWebProfile get() = ChromiumWebProfileStore.instance

actual fun getDwebProfileStoreInstance(): DWebProfileStore = chromiumWebProfile
