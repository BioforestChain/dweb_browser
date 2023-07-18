package info.bagen.dwebbrowser.microService.desktop.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import info.bagen.dwebbrowser.microService.browser.jmm.JmmMetadata

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
  val jmmMetadata: JmmMetadata,
) {
  enum class ScreenType {
    Hide, Half, Full;
  }
}