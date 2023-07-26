package info.bagen.dwebbrowser.microService.core

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import info.bagen.dwebbrowser.base.BaseActivity
import info.bagen.dwebbrowser.microService.browser.jmm.JsMicroModule
import info.bagen.dwebbrowser.microService.mwebview.MultiWebViewController
import org.dweb_browser.helper.*
import org.dweb_browser.microservice.core.NativeMicroModule
import org.dweb_browser.helper.Mmid

abstract class AndroidNativeMicroModule(override val mmid: Mmid) : NativeMicroModule(mmid) {
  protected val installAppList = Companion.installAppList
  protected val runningAppList = Companion.runningAppList

  companion object {
    //  管理所有的activity
    private val activity: BaseActivity? = null

    // 管理所有正在运行的窗口
    internal val runningAppList = mutableStateListOf<WindowAppInfo>()

    // 管理已安装的应用
    internal val installAppList = mutableStateListOf<WindowAppInfo>()
  }

  protected fun getActivity(): BaseActivity ? =activity

  protected val activitySignal = Signal<BaseActivity>()
  fun onActivity(cb: Callback<BaseActivity>) = activitySignal.listen(cb)

  fun getAppWindowMap(mmid: Mmid) : WindowAppInfo? = runningAppList.firstOrNull {
    it.jsMicroModule.mmid == mmid
  }
  protected val windowSignal = Signal<WindowAppInfo>()
  private fun onWindow(cb: Callback<WindowAppInfo>) = windowSignal.listen(cb)

  init {
    onWindow { appInfo ->
      runningAppList.add(appInfo)
      appInfo.viewItem?.webView?.onCloseWindow {
        runningAppList.remove(appInfo)
      }
      return@onWindow true
    }
  }
}

data class WindowAppInfo(
  var expand: Boolean = false, // 用于保存界面状态显示时是半屏还是全屏
  val jsMicroModule: JsMicroModule,
) {
  enum class ScreenType {
    Hide, Half, Full;
  }

  val sort: MutableState<Int> = mutableIntStateOf(0) // 排序，位置
  val screenType: MutableState<ScreenType> = mutableStateOf(ScreenType.Hide) // 默认隐藏
  val offsetX: MutableState<Float> = mutableFloatStateOf(0f) // X轴偏移量
  val offsetY: MutableState<Float> = mutableFloatStateOf(0f) // Y轴偏移量
  val zoom: MutableState<Float> = mutableFloatStateOf(1f) // 缩放

  var viewItem: MultiWebViewController.MultiViewItem? = null
}