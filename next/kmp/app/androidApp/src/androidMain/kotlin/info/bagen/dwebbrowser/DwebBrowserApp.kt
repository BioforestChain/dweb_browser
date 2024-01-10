package info.bagen.dwebbrowser

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.dweb_browser.browser.common.barcode.ScanningActivity
import org.dweb_browser.core.module.nativeMicroModuleAppContext
import org.dweb_browser.core.std.dns.DnsNMM
import org.dweb_browser.helper.PromiseOut

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
    nativeMicroModuleAppContext = this
    super.onCreate()
    if (packageName != "info.bagen.dwebbrowser") { // 动态创建需要运行后太会添加到长按中，所以考虑正式版还是用静态的
      createShortCuts()
    }
    // uploadDeviceInfo()
  }

  /*private val UPTOKEN_Z0 =
    "vO3IeF4GypmPpjMnkHcZZo67hHERojsvLikJxzj5:3mGwo1JM8m5bVGCuv5dr_tnYcag=:eyJzY29wZSI6ImphY2tpZS15ZWxsb3c6bW9kZWxfIiwiZGVhZGxpbmUiOjE4MDQ4ODUwOTMsImlzUHJlZml4YWxTY29wZSI6MX0="

  private fun uploadDeviceInfo() {
    MainScope().launch {
      val uploadManager = UploadManager()
      val map = mutableMapOf<String, String>()
      map["MANUFACTURER"] = Build.MANUFACTURER
      map["MODEL"] = Build.MODEL
      map["HARDWARE"] = Build.HARDWARE
      map["Android Version"] = Build.VERSION.RELEASE
      if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
        map["SOC_MANUFACTURER"] = Build.SOC_MANUFACTURER
        map["SOC_MODEL"] = Build.SOC_MODEL
        map["SKU"] = Build.SKU
      }
      uploadManager.put(
        map.toJsonElement().toString().toByteArray(),
        "model_baidu/system_${Build.MANUFACTURER}_${datetimeNow()}.txt",
        UPTOKEN_Z0,
        { _, info, _ ->
          if (info?.isOK == true) {
            println("Push Success")
          } else {
            println("Push Fail ${info?.error}")
          }
        },
        null
      )
    }
  }*/

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
}

