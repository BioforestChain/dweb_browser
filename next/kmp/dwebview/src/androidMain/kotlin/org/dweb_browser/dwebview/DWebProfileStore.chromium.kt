package org.dweb_browser.dwebview

import android.annotation.SuppressLint
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import org.dweb_browser.dwebview.engine.DWebViewEngine


@SuppressLint("RequiresFeature")
class ChromiumWebProfileStore(private val profileStore: ProfileStore) :
  ProfileStore by profileStore, AndroidWebProfileStore {
  companion object {
    val instance by lazy { ChromiumWebProfileStore(ProfileStore.getInstance()) }
  }

  init {
    for (name in profileStore.allProfileNames.toList()) {
      if (name.endsWith(".incognito")) {
        profileStore.deleteProfile(name)
      }
    }
  }

  override suspend fun getAllProfileNames() = profileStore.allProfileNames
  override suspend fun getOrCreateProfile(engine: DWebViewEngine, profileName: String) =
    profileStore.getOrCreateProfile(profileName).let {
      WebViewCompat.setProfile(engine, profileName)
      ChromiumWebProfile(it)
    }
}