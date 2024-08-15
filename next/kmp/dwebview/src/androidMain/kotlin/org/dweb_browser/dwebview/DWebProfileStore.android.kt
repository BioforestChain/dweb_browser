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
    profileName: ProfileName,
  ): DWebProfile

  fun getOrCreateIncognitoProfile(
    engine: DWebViewEngine,
    profileName: ProfileName,
    sessionId: String,
  ): DWebProfile
}

class CompactDWebProfileStore private constructor() : AndroidWebProfileStore {
  companion object {
    internal val cookieManager by lazy { CookieManager.getInstance() }
    internal val webStorage by lazy { WebStorage.getInstance() }
    internal val geolocationPermissions by lazy { GeolocationPermissions.getInstance() }
    internal val serviceWorkerController by lazy { ServiceWorkerController.getInstance() }

    internal val instance by lazy { CompactDWebProfileStore() }
  }

  override val isSupportIncognitoProfile: Boolean = false
  override fun getOrCreateProfile(
    engine: DWebViewEngine,
    profileName: ProfileName,
  ): DWebProfile = when {
    IDWebView.isEnableProfile -> allProfiles.getOrPut(profileName.key) {
      CompactDWebProfile(profileName).also {
        keyValueStore.setValues(DwebProfilesKey, allProfiles.keys + profileName.key)
      }
    }.also {
      setProfile(it, engine)
    }

    else -> CompactDWebProfile(NoProfileName())
  }

  /**
   * 本身并不支持隐私模式，这里只是强制模拟，禁用了一些数据的写入
   */
  override fun getOrCreateIncognitoProfile(
    engine: DWebViewEngine,
    profileName: ProfileName,
    sessionId: String,
  ): CompactDWebProfile {
    val incognitoProfileName = ProfileIncognitoNameV1.from(profileName, sessionId)

    engine.clearCache(true)
    engine.settings.saveFormData = false
    engine.settings.domStorageEnabled = false
    engine.settings.databaseEnabled = false

    return CompactDWebProfile(incognitoProfileName)
  }

  private val allProfiles by lazy {
    SafeHashMap((keyValueStore.getValues(DwebProfilesKey) ?: setOf()).associateWith {
      CompactDWebProfile(ProfileName.parse(it))
    }.toMutableMap())
  }

  override suspend fun getAllProfileNames() = allProfiles.values.map { it.profileName }.toList()

  private fun setProfile(profile: CompactDWebProfile, engine: DWebViewEngine) {
    profile.profileName.mmid?.also { mmid ->
      val job = engine.lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) {
        val visitedOriginsKey = keyVisitedOrigins(mmid)
        val visitedOrigins = SuspendOnce {
          keyValueStore.getValues(visitedOriginsKey)?.toMutableSet() ?: mutableSetOf()
        }
        val visitedUrlsKey = keyVisitedUrls(mmid)
        val visitedUrls = SuspendOnce {
          keyValueStore.getValues(visitedUrlsKey)?.toMutableSet() ?: mutableSetOf()
        }
        engine.loadStateFlow.collect { state ->
          if (state is WebLoadStartState) {
            val webUrl = state.url.toWebUrl()
            if (webUrl?.host?.endsWith(mmid) == true) {
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
        job.cancel()
        profile.bindingJobs -= job
      }
    }
  }

  private fun keyVisitedUrls(mmid: MMID) = "visitedUrls-$mmid"
  private fun keyVisitedOrigins(mmid: MMID) = "visitedOrigins-$mmid"

  override suspend fun deleteProfile(name: ProfileName) = when (val mmid = name.mmid) {
    null -> false
    else -> withMainContext {
      var success = false
      val visitedOriginsKey = keyVisitedOrigins(mmid)
      keyValueStore.getValues(visitedOriginsKey)?.let { origins ->
        success = true
        for (origin in origins) {
          webStorage.deleteOrigin(origin)
          geolocationPermissions.clear(origin)
        }
      }

      val visitedUrlsKey = keyVisitedUrls(mmid)
      keyValueStore.getValues(visitedUrlsKey)?.let { urls ->
        success = true
        for (url in urls) {
          cookieManager.setCookie(url, "")
        }
      }
      keyValueStore.removeKeys(visitedOriginsKey, visitedUrlsKey)

      allProfiles.remove(name.key)
      keyValueStore.setValues(DwebProfilesKey, allProfiles.keys)

      success
    }
  }
}

/**
 * WARNING 必须在主线程中使用
 */
internal val androidWebProfileStore by lazy {
  when {
    IDWebView.isSupportProfile -> ChromiumWebProfileStore.instance
    else -> CompactDWebProfileStore.instance
  }
}

actual suspend fun getDwebProfileStoreInstance(): DWebProfileStore =
  withMainContext { androidWebProfileStore }
