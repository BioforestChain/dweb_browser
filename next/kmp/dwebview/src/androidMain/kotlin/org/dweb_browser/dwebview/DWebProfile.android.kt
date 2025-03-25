package org.dweb_browser.dwebview


import android.os.CancellationSignal
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.ServiceWorkerController
import android.webkit.WebStorage
import androidx.webkit.OutcomeReceiverCompat
import androidx.webkit.PrefetchException
import androidx.webkit.Profile
import androidx.webkit.SpeculativeLoadingConfig
import androidx.webkit.SpeculativeLoadingParameters
import kotlinx.coroutines.Job
import java.util.concurrent.Executor


@Profile.ExperimentalUrlPrefetch
class CompactDWebProfile internal constructor(override val profileName: ProfileName) : Profile,
  DWebProfile {

  internal val bindingJobs = mutableListOf<Job>()
  override val isIncognito get() = profileName.isIncognito

  override fun getName(): String {
    return this.profileName.key
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

  override fun prefetchUrlAsync(
    url: String,
    cancellationSignal: CancellationSignal?,
    callbackExecutor: Executor,
    operationCallback: OutcomeReceiverCompat<Void?, PrefetchException?>
  ) {

  }

  override fun prefetchUrlAsync(
    url: String,
    cancellationSignal: CancellationSignal?,
    callbackExecutor: Executor,
    speculativeLoadingParameters: SpeculativeLoadingParameters,
    operationCallback: OutcomeReceiverCompat<Void?, PrefetchException?>
  ) {

  }

  override fun clearPrefetchAsync(
    url: String,
    callbackExecutor: Executor,
    operationCallback: OutcomeReceiverCompat<Void?, PrefetchException?>
  ) {

  }

  override fun setSpeculativeLoadingConfig(speculativeLoadingConfig: SpeculativeLoadingConfig) {

  }
}
