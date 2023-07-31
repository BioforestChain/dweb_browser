package info.bagen.dwebbrowser.microService.core

import androidx.compose.runtime.mutableStateListOf
import info.bagen.dwebbrowser.base.BaseActivity
import info.bagen.dwebbrowser.microService.browser.desktop.DeskAppMetaData
import org.dweb_browser.helper.Callback
import org.dweb_browser.helper.Signal
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.microservice.help.MMID

abstract class AndroidNativeMicroModule(override val mmid: MMID, override val name: String) :
  NativeMicroModule(mmid, name) {

  companion object {
    //  管理所有的activity
    private val activity: BaseActivity? = null

    // 管理所有正在运行的窗口
    internal val runningAppList = mutableStateListOf<DeskAppMetaData>()
  }

  protected fun getActivity(): BaseActivity? = activity

  protected val activitySignal = Signal<BaseActivity>()
  fun onActivity(cb: Callback<BaseActivity>) = activitySignal.listen(cb)


  protected val windowSignal = Signal<DeskAppMetaData>()
  private fun onWindow(cb: Callback<DeskAppMetaData>) = windowSignal.listen(cb)

  init {
    onWindow { appInfo ->
      //TODO
//      bootstrapContext.dns.install()
      appInfo.viewItem?.webView?.onCloseWindow {
        runningAppList.remove(appInfo)
      }
      return@onWindow true
    }
  }
}

