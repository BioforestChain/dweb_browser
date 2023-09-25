package org.dweb_browser.browserUI.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import org.dweb_browser.browserUI.download.DwebBrowserService

const val PrivacyUrl = "https://dweb-browser.bagen.info/protocol.html" // 隐私协议地址
const val SupportUrl = "https://dweb-browser.bagen.info/support.html" // webview版本升级帮助界面

class BrowserUIApp private constructor() {

  companion object {
    val Instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
      BrowserUIApp()
    }
  }

  private var mContext: Context? = null

  /// TODO 应该使用 BrowserNMM().appContext 来获取全局上下文
  val appContext
    get() = mContext
      ?: throw Exception("The app context is empty. Please initialize the content in the Application")
  var mBinderService: DwebBrowserService.DwebBrowserBinder? = null
  private var mServiceConnection: MyConnection? = null

  fun setAppContext(context: Context) {
    if (mContext == null) {
      mContext = context
      bindDwebBrowserService()
    }
  }

  fun bindDwebBrowserService() {
    unbindDwebBrowserService() // 释放之前bind的
    MyConnection().apply {
      val bindIntent = Intent(appContext, DwebBrowserService::class.java)
      appContext.bindService(bindIntent, this, Context.BIND_AUTO_CREATE)
      mServiceConnection = this
    }
  }

  fun unbindDwebBrowserService() {
    mServiceConnection?.let { appContext.unbindService(it) }
  }

  internal inner class MyConnection : ServiceConnection {
    override fun onServiceDisconnected(p0: ComponentName?) {
      mBinderService = null
    }

    override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
      mBinderService = p1 as DwebBrowserService.DwebBrowserBinder//对象的强制转换
    }
  }

}