package info.bagen.dwebbrowser.microService.desktop.model

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import org.dweb_browser.helper.AppMetaData

val LocalInstallList = compositionLocalOf<MutableList<AppInfo>> {
  noLocalProvidedFor("LocalInstallList")
}

val LocalOpenList = compositionLocalOf<MutableList<AppInfo>> {
  noLocalProvidedFor("LocalOpenList")
}

private fun noLocalProvidedFor(name: String): Nothing {
  error("CompositionLocal $name not present")
}

data class AppInfo(
  val sort: MutableState<Int> = mutableIntStateOf(0), // 排序，位置
  var expand: Boolean = false, // 用于保存界面状态显示时是半屏还是全屏
  val screenType: MutableState<ScreenType> = mutableStateOf(ScreenType.Hide), // 默认隐藏
  val offsetX: MutableState<Float> = mutableFloatStateOf(0f), // X轴偏移量
  val offsetY: MutableState<Float> = mutableFloatStateOf(0f), // Y轴偏移量
  val zoom: MutableState<Float> = mutableFloatStateOf(1f), // 缩放
  val appMetaData: AppMetaData,
) {
  enum class ScreenType {
    Hide, Half, Full;
  }
}

val LocalDrawerManager = compositionLocalOf {
  DrawerManager()
}

class DrawerManager {
  val visibleState: MutableTransitionState<Boolean> = MutableTransitionState(true)
  fun hide() {
    visibleState.targetState = false
  }
  fun show() {
    visibleState.targetState = true
  }
}