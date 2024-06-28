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
import org.dweb_browser.helper.getAppContext
import org.dweb_browser.helper.platform.keyValueStore
import org.dweb_browser.helper.saveStringSet
import org.dweb_browser.helper.toWebUrl
import org.dweb_browser.helper.withMainContext

internal const val DwebProfilesKey = "dweb-profiles"

interface AndroidWebProfileStore : DWebProfileStore {
  val isSupportIncognitoProfile: Boolean
  fun getOrCreateProfile(
    engine: DWebViewEngine,
    profileName: String = engine.remoteMM.mmid,
  ): DWebProfile

  fun getOrCreateIncognitoProfile(
    engine: DWebViewEngine,
    sessionId: String,
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

  override val isSupportIncognitoProfile: Boolean = false
  override fun getOrCreateProfile(
    engine: DWebViewEngine,
    profileName: String,
  ): DWebProfile = allProfiles.getOrPut(profileName) {
    CompactDWebProfile(profileName).also {
      keyValueStore.setValues(DwebProfilesKey, allProfiles.keys + profileName)
    }
  }.also {
    setProfile(it, engine)
  }

  /**
   * 本身并不支持隐私模式，这里只是强制模拟，禁用了一些数据的写入
   */
  override fun getOrCreateIncognitoProfile(
    engine: DWebViewEngine,
    sessionId: String,
    profileName: String,
  ): CompactDWebProfile {
    val incognitoProfileName = "$profileName@$sessionId$IncognitoSuffix"

    engine.clearCache(true)
    engine.settings.saveFormData = false
    engine.settings.domStorageEnabled = false
    engine.settings.databaseEnabled = false

    return CompactDWebProfile(incognitoProfileName)
  }

  private val allProfiles by lazy {
    SafeHashMap((keyValueStore.getValues(DwebProfilesKey) ?: setOf()).associateWith {
      CompactDWebProfile(it)
    }.toMutableMap())
  }

  override suspend fun getAllProfileNames() = allProfiles.keys.toList()

  private fun setProfile(profile: CompactDWebProfile, engine: DWebViewEngine) {
    if (profile.profileName.endsWith(".dweb")) {
      val job = engine.lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) {
        val visitedOriginsKey = keyVisitedOrigins(profile.profileName)
        val visitedOrigins = SuspendOnce {
          keyValueStore.getValues(visitedOriginsKey)?.toMutableSet() ?: mutableSetOf()
        }
        val visitedUrlsKey = keyVisitedUrls(profile.profileName)
        val visitedUrls = SuspendOnce {
          keyValueStore.getValues(visitedUrlsKey)?.toMutableSet() ?: mutableSetOf()
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
        profile.bindingJobs -= job
      }
    }
  }

  private fun keyVisitedUrls(mmid: MMID) = "visitedUrls-$mmid"
  private fun keyVisitedOrigins(mmid: MMID) = "visitedOrigins-$mmid"

  override suspend fun deleteProfile(name: String) = when {
    name.endsWith(".dweb") -> withMainContext {
      var success = false
      val visitedOriginsKey = keyVisitedOrigins(name)
      keyValueStore.getValues(visitedOriginsKey)?.let { origins ->
        success = true
        for (origin in origins) {
          webStorage.deleteOrigin(origin)
          geolocationPermissions.clear(origin)
        }
      }

      val visitedUrlsKey = keyVisitedUrls(name)
      keyValueStore.getValues(visitedUrlsKey)?.let { urls ->
        success = true
        for (url in urls) {
          cookieManager.setCookie(url, "")
        }
      }
      keyValueStore.removeKeys(visitedOriginsKey, visitedUrlsKey)

      allProfiles.remove(name)
      keyValueStore.setValues(DwebProfilesKey, allProfiles.keys)

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
