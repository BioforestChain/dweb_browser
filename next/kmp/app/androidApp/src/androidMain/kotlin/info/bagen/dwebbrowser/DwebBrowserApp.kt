package info.bagen.dwebbrowser

import android.app.Application
import android.content.Context
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.androidAppContextDeferred

class DwebBrowserApp : Application() {
  companion object {
    lateinit var appContext: Context

    private var dnsNMMPo: PromiseOut<DnsNMM>? = null
    fun startMicroModuleProcess(): PromiseOut<DnsNMM> = synchronized(this) {
      if (dnsNMMPo == null) {
        dnsNMMPo = PromiseOut<DnsNMM>().also { dnsNMMPo ->
          MainScope().launch {
            try {
              val dnsNMM = startDwebBrowser()
              dnsNMMPo.resolve(dnsNMM)
            } catch (e: Throwable) {
              dnsNMMPo.reject(e)
            }
          }
        }
      } else {
        MainScope().launch {
          dnsNMMPo!!.waitPromise().bootstrap()
        }
      }
      return dnsNMMPo!!
    }
  }

  override fun onCreate() {
    appContext = this
    androidAppContextDeferred.complete(this)
    super.onCreate()
    // uploadDeviceInfo()
  }

  override fun onTerminate() {
    super.onTerminate()
  }
}

