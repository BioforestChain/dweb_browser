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
import org.dweb_browser.helper.SafeHashMap
import org.dweb_browser.helper.SuspendOnce
import org.dweb_browser.helper.appKvGetValues
import org.dweb_browser.helper.appKvRemoveValues
import org.dweb_browser.helper.appKvSetValues
import org.dweb_browser.helper.getAppContext
import org.dweb_browser.helper.saveStringSet
import org.dweb_browser.helper.toWebUrl
import org.dweb_browser.helper.withMainContext


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
      appKvSetValues("dweb-profiles", allProfiles.keys + profileName)
    }
  }.also {
    setProfile(it, engine)
  }

  private val allProfiles by lazy {
    SafeHashMap((appKvGetValues("dweb-profiles") ?: setOf()).associateWith {
      CompactDWebProfile(it)
    }.toMutableMap())
  }

  override suspend fun getAllProfileNames() = allProfiles.keys.toList()

  private fun setProfile(profile: CompactDWebProfile, engine: DWebViewEngine) {
    if (profile.profileName.endsWith(".dweb")) {
      val job = engine.lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) {
        val visitedOriginsKey = keyVisitedOrigins(profile.profileName)
        val visitedOrigins = SuspendOnce {
          appKvGetValues(visitedOriginsKey)?.toMutableSet() ?: mutableSetOf()
        }
        val visitedUrlsKey = keyVisitedUrls(profile.profileName)
        val visitedUrls = SuspendOnce {
          appKvGetValues(visitedUrlsKey)?.toMutableSet() ?: mutableSetOf()
        }
        engine.dWebViewClient.loadStateChangeSignal.listen { state ->
          if (state is WebLoadStartState) {
            val webUrl = state.url.toWebUrl()
            if (webUrl?.host?.endsWith(profile.profileName) == true) {
              val origin = webUrl.protocolWithAuthority + "/"
              visitedUrls().also {
                if (it.add(origin)) {
                  getAppContext().saveStringSet(visitedUrlsKey, it)
                }
              }
              visitedOrigins().also {
                if (it.add(origin)) {
                  getAppContext().saveStringSet(visitedOriginsKey, it)
                }
              }
            }
          }
        }
      }

      profile.bindingJobs += job
      engine.destroyStateSignal.onDestroy {
        println("QAQ unbindingJobs ${profile.profileName}")
        profile.bindingJobs -= job
      }
    }
  }

  private fun keyVisitedUrls(mmid: MMID) = "visitedUrls-$mmid"
  private fun keyVisitedOrigins(mmid: MMID) = "visitedOrigins-$mmid"

  override suspend fun deleteProfile(name: String) = when {
    name.endsWith(".dweb") -> withMainContext {
      println("QAQ deleteProfile name=$name")
      var success = false
      val visitedOriginsKey = keyVisitedOrigins(name)
      appKvGetValues(visitedOriginsKey)?.let { origins ->
        println("QAQ deleteProfile visitedOrigins=${origins.joinToString(";")}")
        success = true
        for (origin in origins) {
          webStorage.deleteOrigin(origin)
          geolocationPermissions.clear(origin)
        }
      }

      val visitedUrlsKey = keyVisitedUrls(name)
      appKvGetValues(visitedUrlsKey)?.let { urls ->
        println("QAQ deleteProfile visitedUrls=${urls.joinToString(";")}")
        success = true
        for (url in urls) {
          cookieManager.setCookie(url, "")
        }
      }
      appKvRemoveValues(visitedOriginsKey, visitedUrlsKey)

      allProfiles.remove(name)
      appKvSetValues("dweb-profiles", allProfiles.keys)

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
