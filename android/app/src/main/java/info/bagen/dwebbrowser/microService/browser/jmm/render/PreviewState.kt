package info.bagen.dwebbrowser.microService.browser.jmm.render

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset

internal data class PreviewState(
  val showPreview: MutableTransitionState<Boolean> = MutableTransitionState(false), // 用于判断是否显示预览界面
  val selectIndex: MutableState<Int> = mutableStateOf(0), // 用于保存当前选中的图片下标
  //val firstVisible: MutableState<Int> = mutableStateOf(0), // 用于记录第一个有效显示的照片
  //val firstVisibleOffset: MutableState<Int> = mutableStateOf(0), // 用于记录第一个有效显示的照片偏移量
  val offset: MutableState<Offset> = mutableStateOf(Offset.Zero), // 用于保存当前选中图片的中心坐标
  var imageLazy: LazyListState? = null,
  var outsideLazy: LazyListState,
  var screenWidth: Int,
  var screenHeight: Int,
  var statusBarHeight: Int,
  var density: Float,
)