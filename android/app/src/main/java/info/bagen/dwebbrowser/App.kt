package info.bagen.dwebbrowser

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import info.bagen.dwebbrowser.microService.startDwebBrowser
import org.dweb_browser.microservice.sys.dns.DnsNMM
import info.bagen.dwebbrowser.util.PlaocUtil
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.dweb_browser.browserUI.util.BrowserUIApp
import org.dweb_browser.helper.*

class App : Application() {
  companion object {
    lateinit var appContext: Context

    val grant = PromiseOut<Boolean>()

    @OptIn(DelicateCoroutinesApi::class)
    fun <T> startActivity(cls: Class<T>, onIntent: (intent: Intent) -> Unit) {
      GlobalScope.launch(ioAsyncExceptionHandler) {
        if (!grant.waitPromise()) {
          /// TODO 用户拒绝协议应该做的事情
          return@launch
        }

        val intent = Intent(appContext, cls)
        onIntent(intent)
        appContext.startActivity(intent)
      }
    }


    private val dnsNMMPo = PromiseOut<DnsNMM>()
    fun startMicroModuleProcess() {
      if (dnsNMMPo.isResolved) {
        runBlockingCatching {
          dnsNMMPo.value!!.bootstrap()
        }.getOrThrow()
      } else {
        synchronized(dnsNMMPo) {
          val dnsNMM = runBlockingCatching {
            startDwebBrowser()
          }.getOrThrow()
          dnsNMMPo.resolve(dnsNMM)
        }
      }
    }
  }

  override fun onCreate() {
    super.onCreate()
    appContext = this
    PlaocUtil.addShortcut(this) // 添加桌面快捷方式
    // startService(Intent(this@App, DwebBrowserService::class.java))
    // DwebBrowserUtil.INSTANCE.bindDwebBrowserService()
    BrowserUIApp.Instance.setAppContext(this) // 初始化BrowserUI模块
  }


  private class ActivityLifecycleCallbacksImp : ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
      // android10中规定, 只有默认输入法(IME)或者是目前处于焦点的应用, 才能访问到剪贴板数据，所以要延迟到聚焦后
      /*activity.window.decorView.post {
          GlobalScope.launch { ClipboardUtil.readAndParsingClipboard(activity) }
      }*/
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }
  }
}

