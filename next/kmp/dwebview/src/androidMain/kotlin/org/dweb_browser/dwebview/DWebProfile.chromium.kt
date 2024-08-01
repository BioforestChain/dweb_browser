package org.dweb_browser.dwebview


import android.annotation.SuppressLint
import androidx.webkit.Profile
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import org.dweb_browser.dwebview.engine.DWebViewEngine

@SuppressLint("RequiresFeature")
class ChromiumWebProfile internal constructor(override val profileName: ProfileName) : DWebProfile {
  val isEnabled get() = profile != null
  var profile: Profile? = null
  fun enable(profileStore: ProfileStore, engine: DWebViewEngine) {
    if (isEnabled) {
      return
    }
    // 不论 isIncognito 与否。都使用同样的方式创建 profile
    profile = profileStore.getOrCreateProfile(profileName.key)
    WebViewCompat.setProfile(engine, profileName.key)
  }

  //  override val profileName by lazy { ProfileName.parse(profile.name) }
  override val isIncognito get() = profileName.isIncognito
}
