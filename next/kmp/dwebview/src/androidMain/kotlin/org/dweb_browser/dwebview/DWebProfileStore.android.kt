package org.dweb_browser.dwebview

import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.ServiceWorkerController
import android.webkit.WebStorage
import io.ktor.http.protocolWithAuthority
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.getAppContext
import org.dweb_browser.helper.getAppContextUnsafe
import org.dweb_browser.helper.getStringSet
import org.dweb_browser.helper.saveStringSet
import org.dweb_browser.helper.toWebUrl


interface AndroidWebProfileStore : DWebProfileStore {
  fun getOrCreateProfile(
    engine: DWebViewEngine,
    profileName: String = engine.remoteMM.mmid,
  ): DWebProfile
}

class CompactDWebProfileStore private constructor() : AndroidWebProfileStore {
  companion object {
    internal val cookieManager by lazy { CookieManager.getInstance() }
    internal val webStorage by lazy { WebStorage.getInstance() }
    internal val geolocationPermissions by lazy { GeolocationPermissions.getInstance() }
    internal val serviceWorkerController by lazy { ServiceWorkerController.getInstance() }

    val instance by lazy { CompactDWebProfileStore() }
  }

  override fun getOrCreateProfile(
    engine: DWebViewEngine,
    profileName: String,
  ): DWebProfile = allProfiles.getOrPut(profileName) {
    CompactDWebProfile(profileName).also {
      getAppContextUnsafe().saveStringSet("dweb-profiles", allProfiles.keys + profileName)
    }
  }.also {
    setProfile(it, engine)
  }

  private val allProfiles by lazy {
    (getAppContextUnsafe().getStringSet("dweb-profiles") ?: setOf()).associateWith {
      CompactDWebProfile(it)
    }.toMutableMap()
  }

  override suspend fun getAllProfileNames() = allProfiles.keys.toList()

  private fun setProfile(profile: DWebProfile, engine: DWebViewEngine) {
    if (profile.profileName.endsWith(".dweb")) {
      engine.lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) {
        val storeKey = keyVisitedUrls(profile.profileName)
        val visitedUrls = SuspendOnce {
          getValues(storeKey)?.toMutableSet() ?: mutableSetOf()
        }
        engine.dWebViewClient.loadStateChangeSignal.listen {
          if (it is WebLoadStartState) {
            val webUrl = it.url.toWebUrl()
            if (webUrl?.host?.endsWith(profile.profileName) == true) {
              visitedUrls().apply {
                if (add(webUrl.protocolWithAuthority)) {
                  getAppContext().saveStringSet(storeKey, this)
                }
              }
            }
          }
        }
      }
    }
  }

  private fun keyVisitedUrls(mmid: MMID) = "visitedUrls-$mmid"
  private fun keyVisitedOrigins(mmid: MMID) = "visitedOrigins-$mmid"
  private fun getValues(storeKey: String) = getAppContextUnsafe().getStringSet(storeKey)

  override suspend fun deleteProfile(name: String) = when {
    name.endsWith(".dweb") -> {
      var success = false
      val visitedOriginsKey = keyVisitedOrigins(name)
      getValues(visitedOriginsKey)?.let { origins ->
        success = true
        for (origin in origins) {
          webStorage.deleteOrigin(origin)
          geolocationPermissions.clear(origin)
        }
      }
      val visitedUrlsKey = keyVisitedUrls(name)
      getValues(visitedUrlsKey)?.let { urls ->
        success = true
        for (url in urls) {
          cookieManager.setCookie(url, "")
        }
      }
      success
    }

    else -> false
  }

}

internal val androidWebProfileStore by lazy {
  when {
    IDWebView.isSupportProfile -> ChromiumWebProfileStore.instance
    else -> CompactDWebProfileStore.instance
  }
}

actual fun getDwebProfileStoreInstance(): DWebProfileStore = androidWebProfileStore
