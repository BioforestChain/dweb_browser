package info.bagen.dwebbrowser

import android.app.Application
import android.content.Context
import android.webkit.WebView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.browser.DwebBrowserLauncher
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.helper.androidAppContextDeferred
import org.dweb_browser.helper.compose.ENV_SWITCH_KEY
import org.dweb_browser.helper.compose.envSwitch

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

    private suspend fun startApplication(): DnsNMM {
      /// 启动Web调试
      WebView.setWebContentsDebuggingEnabled(true)

      /// Android版本默认启用新版桌面
      envSwitch.init(ENV_SWITCH_KEY.DESKTOP_STYLE_COMPOSE) { "true" }

      val launcher = DwebBrowserLauncher(if (BuildConfig.DEBUG) "/.+/" else null)
      launcher.launch()
      return launcher.dnsNMM
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

