package info.bagen.dwebbrowser

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import info.bagen.dwebbrowser.microService.startDwebBrowser
import info.bagen.dwebbrowser.microService.sys.barcodeScanning.ScanningActivity
import info.bagen.dwebbrowser.util.CrashUtil
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.dweb_browser.browserUI.util.BrowserUIApp
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.runBlockingCatching
import org.dweb_browser.microservice.core.AndroidNativeMicroModule
import org.dweb_browser.microservice.sys.dns.DnsNMM

class App : Application() {
  companion object {
    lateinit var appContext: Context

    var grant: PromiseOut<Boolean>? = null
    private val lockActivityState = Mutex()
    private val ioAsyncScope = MainScope() + ioAsyncExceptionHandler

    fun <T> startActivity(cls: Class<T>, onIntent: (intent: Intent) -> Unit) {
      ioAsyncScope.launch {
        lockActivityState.withLock {
          if (grant?.waitPromise() == false) {
            return@withLock // TODO 用户拒绝协议应该做的事情
          }

          val intent = Intent(appContext, cls).also {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            it.`package` = appContext.packageName
          }
          onIntent(intent)
          appContext.startActivity(intent)
        }
      }
    }

    private val dnsNMMPo = PromiseOut<DnsNMM>()
    fun startMicroModuleProcess(): DnsNMM {
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
      return dnsNMMPo.value!!
    }
  }

  override fun onCreate() {
    super.onCreate()
    appContext = this
    // CrashUtil.instance.init(this) // 初始化捕获异常
    // PlaocUtil.addShortcut(this) // 添加桌面快捷方式
    // startService(Intent(this@App, DwebBrowserService::class.java))
    // DwebBrowserUtil.INSTANCE.bindDwebBrowserService()
    BrowserUIApp.Instance.setAppContext(this) // 初始化BrowserUI模块
    AndroidNativeMicroModule.appContext = this
    if (packageName != "info.bagen.dwebbrowser") { // 动态创建需要运行后太会添加到长按中，所以考虑正式版还是用静态的
      createShortCuts()
    }
  }

  /**
   * 原先想作为静态入口的，配置在 shortcut.xml 中，后面发现 android:targetPackage是字符串，没法动态配置包名
   * 包名由于debug有执行了特殊处理，导致静态的包名不适用，所以使用动态创建 shortcut 的方式
   */
  private fun createShortCuts() {
    val build = ShortcutInfoCompat.Builder(this, "dweb_qrcode_debug")
      .setShortLabel(getString(R.string.shortcut_short_label))
      .setLongLabel(getString(R.string.shortcut_long_label))
      .setIcon(IconCompat.createWithResource(this, R.drawable.ic_main_qrcode_scan))
      .setIntent(Intent(this, ScanningActivity::class.java).also {
        it.action = Intent.ACTION_VIEW
        it.putExtra(ScanningActivity.IntentFromIPC, false)
      })
      .build()
    ShortcutManagerCompat.pushDynamicShortcut(this, build)
  }

  override fun onTerminate() {
    super.onTerminate()
    ioAsyncScope.cancel()
  }

  private class ActivityLifecycleCallbacksImp : ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {

      // android10中规定, 只有默认输入法(IME)或者是目前处于焦点的应用, 才能访问到剪贴板数据，所以要延迟到聚焦后
      /*activity.window.decorView.post {
          MainScope().launch(ioAsyncExceptionHandler) { ClipboardUtil.readAndParsingClipboard(activity) }
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

