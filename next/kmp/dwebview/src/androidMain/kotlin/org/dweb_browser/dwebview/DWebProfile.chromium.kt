package org.dweb_browser.dwebview


import android.annotation.SuppressLint
import androidx.webkit.Profile

@JvmInline
value class ChromiumWebProfile internal constructor(private val profile: Profile) :
  Profile by profile,
  DWebProfile {
  override val profileName: String
    @SuppressLint("RequiresFeature")
    get() = profile.name
}
