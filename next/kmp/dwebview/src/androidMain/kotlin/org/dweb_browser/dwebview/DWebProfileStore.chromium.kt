package org.dweb_browser.dwebview

import android.annotation.SuppressLint
import androidx.webkit.ProfileStore
import org.dweb_browser.dwebview.engine.DWebViewEngine
import org.dweb_browser.helper.platform.keyValueStore
import org.dweb_browser.helper.withMainContext


/**
 * WARN 必须在主线程中使用，包括构建工作
 */
@SuppressLint("RequiresFeature")
class ChromiumWebProfileStore(private val profileStore: ProfileStore) : AndroidWebProfileStore {
  companion object {
    internal val instance by lazy { ChromiumWebProfileStore(ProfileStore.getInstance()) }
  }

  /**
   * 在本次会话启动阶段，需要遍历清理掉上次会话残留下来的 临时profile
   */
  init {
    for (name in profileStore.allProfileNames.toList()) {
      /// 上个版本的隐私模式使用 incognito_ 前缀，但几乎没有生效过
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
      ?: setOf()).associateWith { ChromiumWebProfile(ProfileName.parse(it)) }.toMutableMap()
  }

  override suspend fun getAllProfileNames() = allProfiles.values.map { it.profileName }.toList()
  override val isSupportIncognitoProfile: Boolean = true
  override fun getOrCreateProfile(engine: DWebViewEngine, profileName: ProfileName): DWebProfile =
    /// 上个版本(240622)起，已经在使用 profileStore 来创建并使用 profile 了
    allProfiles[profileName.key] ?: run {
      ChromiumWebProfile(profileName).also { profile ->
        allProfiles[profileName.key] = profile
        keyValueStore.setValues(DwebProfilesKey, allProfiles.keys)
      }
    }.also { profile ->
      profile.enable(profileStore, engine)
    }

  override fun getOrCreateIncognitoProfile(
    engine: DWebViewEngine,
    profileName: ProfileName,
    sessionId: String,
  ): ChromiumWebProfile {
    /// 上个版本(240622)的 incognito 模式使用更简单的 prefix 模式，但几乎没有生效过
    val incognitoProfileName = ProfileIncognitoNameV1.from(profileName, sessionId)

    return ChromiumWebProfile(incognitoProfileName).also { profile ->
      profile.enable(profileStore, engine)
    }
  }

  override suspend fun deleteProfile(name: ProfileName): Boolean = withMainContext {
    if (!allProfiles.containsKey(name.key)) false
    else when (val profile =
      allProfiles.remove(name.key)?.profile ?: profileStore.getProfile(name.key)) {
      null -> false
      else -> {
        keyValueStore.setValues(DwebProfilesKey, allProfiles.keys)
        /**
         * 官方 profileStore 一旦执行了 setProfile，那么在这次运行期间，执行 deleteProfile 就会说正在占用无法删除
         *
         * 此时我们不删除这个profile，而是直接做清理
         */
        runCatching {
          profileStore.deleteProfile(name.key)
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