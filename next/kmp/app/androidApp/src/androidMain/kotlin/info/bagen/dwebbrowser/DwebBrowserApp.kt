package info.bagen.dwebbrowser

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.helper.androidAppContextDeferred

class DwebBrowserApp : Application() {
  companion object {
    lateinit var appContext: Context
    private var dnsNMMPo = CompletableDeferred<DnsNMM>()
    fun startMicroModuleProcess(): CompletableDeferred<DnsNMM> = synchronized(this) {
      MainScope().launch {
        if (dnsNMMPo.isCompleted) {
          dnsNMMPo.await().bootstrap()
        } else {
          try {
            val dnsNMM = startApplication()
            dnsNMMPo.complete(dnsNMM)
          } catch (e: Throwable) {
            dnsNMMPo.completeExceptionally(e)
          }
        }
      }
      return dnsNMMPo
    }
  }

  override fun onCreate() {
    appContext = this
    androidAppContextDeferred.complete(this)
    super.onCreate()
  }

  override fun onTerminate() {
    super.onTerminate()
  }
}

