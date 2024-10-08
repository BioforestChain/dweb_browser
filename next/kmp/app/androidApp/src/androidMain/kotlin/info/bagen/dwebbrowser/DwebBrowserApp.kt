package info.bagen.dwebbrowser

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.helper.androidAppContextDeferred

class DwebBrowserApp : Application() {
  companion object {
    lateinit var appContext: Context
    private var dnsNMMPo = CompletableDeferred<DnsNMM>()
    private val mutex = Mutex()
    fun startMicroModuleProcess(): CompletableDeferred<DnsNMM> {
      MainScope().launch {
        // 防止在启动过程中重复调用，创建多次，保证幂等性
        mutex.withLock {
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

