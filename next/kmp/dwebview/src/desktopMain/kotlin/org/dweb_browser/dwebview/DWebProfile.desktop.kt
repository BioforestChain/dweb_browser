package org.dweb_browser.dwebview

import com.teamdev.jxbrowser.profile.Profile

class ChromiumWebProfile(internal val profile: Profile, override val profileName: String) :
  DWebProfile {
  internal val engine get() = profile.engine()
}
