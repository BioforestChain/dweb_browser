package org.dweb_browser.dwebview

import org.dweb_browser.platform.desktop.webview.jxBrowserEngine

class ChromiumWebProfileStore private constructor() : DWebProfileStore {
  companion object {
    internal val instance by lazy { ChromiumWebProfileStore() }
  }

  fun getAllProfiles() = mutableMapOf<String, ChromiumWebProfile>().also { result ->
    jxBrowserEngine.allEngines.values.map { engine ->
      for (profile in engine.profiles().list()) {
        if (profile.isIncognito || profile.isDefault) {
          continue
        }
        val profileName = profile.name()
        result[profileName] = ChromiumWebProfile(profile, ProfileName.parse(profileName))
      }
    }
  }

  override suspend fun getAllProfileNames() =
    getAllProfiles().values.map { it.profileName }.toList()

  override suspend fun deleteProfile(name: ProfileName): Boolean {
    return getAllProfiles()[name.key]?.let { item ->
      item.engine.profiles().delete(item.profile)
      true
    } ?: false
  }
}

internal val chromiumWebProfile get() = ChromiumWebProfileStore.instance

actual suspend fun getDwebProfileStoreInstance(): DWebProfileStore = chromiumWebProfile
