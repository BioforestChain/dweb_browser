package info.bagen.dwebbrowser.microService.browser.desktop

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import info.bagen.dwebbrowser.microService.browser.mwebview.MultiWebViewController
import org.dweb_browser.microservice.help.MicroModuleManifest


data class DeskAppMetaData(
  val jsMetaData: MicroModuleManifest,
  val isRunning: Boolean = false, // 是否正在运行
  var isExpand: Boolean = false, // 是否默认展开窗口
)  {


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


