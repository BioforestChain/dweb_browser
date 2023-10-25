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
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.nativeMicroModuleAppContext
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.helper.PromiseOut

class App : Application() {
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
    nativeMicroModuleAppContext = this
    super.onCreate()
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

