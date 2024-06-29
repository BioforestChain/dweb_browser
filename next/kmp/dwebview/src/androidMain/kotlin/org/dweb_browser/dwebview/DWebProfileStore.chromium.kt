package org.dweb_browser.dwebview

import android.annotation.SuppressLint
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.compose.ENV_SWITCH_KEY
import org.dweb_browser.helper.compose.envSwitch
import org.dweb_browser.helper.platform.keyValueStore
import org.dweb_browser.helper.withMainContext


@SuppressLint("RequiresFeature")
class ChromiumWebProfileStore(private val profileStore: ProfileStore) : AndroidWebProfileStore {
  companion object {
    val instance by lazy { ChromiumWebProfileStore(ProfileStore.getInstance()) }
  }

  /**
   * 在本次会话启动阶段，需要遍历清理掉上次会话残留下来的 临时profile
   */
  init {
    for (name in profileStore.allProfileNames.toList()) {
      if (name.endsWith(IncognitoSuffix)) {
        profileStore.deleteProfile(name)
      }
    }
  }

  /**
   * 因为官方 profileStore 一旦执行了 setProfile，那么在这次运行期间，就不能执行 deleteProfile
   * 所以我们自己手动维护一个map，来管理逻辑意义上的清除
   */
  private val allProfiles by lazy {
    (keyValueStore.getValues(DwebProfilesKey)
      ?: setOf()).associateWith { null as ChromiumWebProfile? }.toMutableMap()
  }

  override suspend fun getAllProfileNames() = allProfiles.keys.toList()
  override val isSupportIncognitoProfile: Boolean = true
  override fun getOrCreateProfile(engine: DWebViewEngine, profileName: String): DWebProfile = when {
    envSwitch.isEnabled(ENV_SWITCH_KEY.DWEBVIEW_PROFILE) ->
      (allProfiles[profileName] ?: profileStore.getOrCreateProfile(profileName)).let { profile ->
        WebViewCompat.setProfile(engine, profileName)
        ChromiumWebProfile(profile).also {
          allProfiles[profileName] = it
          keyValueStore.setValues(DwebProfilesKey, allProfiles.keys)
        }
      }

    else -> CompactDWebProfile("*")
  }

  override fun getOrCreateIncognitoProfile(
    engine: DWebViewEngine,
    sessionId: String,
    profileName: String,
  ): ChromiumWebProfile {
    val incognitoProfileName = "$profileName@$sessionId$IncognitoSuffix"
    return (profileStore.getOrCreateProfile(incognitoProfileName)).let { profile ->
      WebViewCompat.setProfile(engine, incognitoProfileName)
      ChromiumWebProfile(profile)
    }
  }

  override suspend fun deleteProfile(name: String): Boolean = withMainContext {
    if (!allProfiles.containsKey(name)) false
    else when (val profile = allProfiles.remove(name) ?: profileStore.getProfile(name)) {
      null -> false
      else -> {
        keyValueStore.setValues(DwebProfilesKey, allProfiles.keys)
        /**
         * 官方 profileStore 一旦执行了 setProfile，那么在这次运行期间，执行 deleteProfile 就会说正在占用无法删除
         *
         * 此时我们不删除这个profile，而是直接做清理
         */
        runCatching {
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
}