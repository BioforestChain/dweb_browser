package org.dweb_browser.dwebview


import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.ServiceWorkerController
import android.webkit.WebStorage
import androidx.webkit.Profile
import kotlinx.coroutines.Job

internal const val IncognitoSuffix = ".incognito"

class CompactDWebProfile internal constructor(override val profileName: String) : Profile,
  DWebProfile {

  internal val bindingJobs = mutableListOf<Job>()
  override val isIncognito get() = profileName.endsWith(IncognitoSuffix)

  override fun getName(): String {
    return this.profileName
  }

  override fun getCookieManager(): CookieManager {
    return CompactDWebProfileStore.cookieManager
  }

  override fun getWebStorage(): WebStorage {
    return CompactDWebProfileStore.webStorage
  }

  override fun getGeolocationPermissions(): GeolocationPermissions {
    return CompactDWebProfileStore.geolocationPermissions
  }

  override fun getServiceWorkerController(): ServiceWorkerController {
    return CompactDWebProfileStore.serviceWorkerController
  }
}
