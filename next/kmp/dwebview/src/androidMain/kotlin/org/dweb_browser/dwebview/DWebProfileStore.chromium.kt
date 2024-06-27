package org.dweb_browser.dwebview

import android.annotation.SuppressLint
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.appKvGetValues
import org.dweb_browser.helper.appKvSetValues
import org.dweb_browser.helper.withMainContext


@SuppressLint("RequiresFeature")
class ChromiumWebProfileStore(private val profileStore: ProfileStore) :
  AndroidWebProfileStore {
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

  /**
   * 因为官方 profileStore 一旦执行了 setProfile，那么在这次运行期间，就不能执行 deleteProfile
   * 所以我们自己手动维护一个map，来管理逻辑意义上的清除
   */
  private val allProfiles by lazy {
    (appKvGetValues("dweb-profiles") ?: setOf()).associateWith { null as ChromiumWebProfile? }
      .toMutableMap()
  }

  override suspend fun getAllProfileNames() = allProfiles.keys.toList()
  override fun getOrCreateProfile(engine: DWebViewEngine, profileName: String) =
    (allProfiles[profileName] ?: profileStore.getOrCreateProfile(profileName)).let { profile ->
      WebViewCompat.setProfile(engine, profileName)
      ChromiumWebProfile(profile).also {
        allProfiles[profileName] = it
        appKvSetValues("dweb-profiles", allProfiles.keys)
      }
    }

  override suspend fun deleteProfile(name: String): Boolean = withMainContext {
    when (val profile = allProfiles.remove(name)) {
      null -> false
      else -> runCatching {
        profileStore.deleteProfile(name)
      }.getOrElse {
        profile.webStorage.deleteAllData()
        profile.cookieManager.removeAllCookies {}
        profile.geolocationPermissions.clearAll()
        true
      }
    }
  }
}