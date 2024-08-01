package org.dweb_browser.dwebview

import com.teamdev.jxbrowser.profile.Profile

class ChromiumWebProfile(internal val profile: Profile, override val profileName: ProfileName) :
  DWebProfile {
  internal val engine get() = profile.engine()
  override val isIncognito get() = profile.isIncognito
}
