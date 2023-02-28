package info.bagen.rust.plaoc.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.service.DwebBrowserService

class DwebBrowserUtil {
  var mBinderService: DwebBrowserService.DwebBrowserBinder? = null
  private var mServiceConnection: MyConnection? = null

  companion object {
    val INSTANCE: DwebBrowserUtil by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
      DwebBrowserUtil()
    }
  }

  fun bindDwebBrowserService() {
    unbindDwebBrowserService() // 释放之前bind的
    MyConnection().apply {
      val bindIntent = Intent(App.appContext, DwebBrowserService::class.java)
      App.appContext.bindService(bindIntent, this, Context.BIND_AUTO_CREATE)
      mServiceConnection = this
    }
  }

  fun unbindDwebBrowserService() {
    mServiceConnection?.let { App.appContext.unbindService(it) }
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