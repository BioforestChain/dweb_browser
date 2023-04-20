package info.bagen.dwebbrowser.ui.browser.android

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.bagen.dwebbrowser.ui.browser.ios.*

private val dimenTextFieldFontSize = 16.sp
private val dimenSearchHorizontalAlign = 5.dp
private val dimenSearchVerticalAlign = 10.dp
private val dimenSearchRoundedCornerShape = 8.dp
private val dimenShadowElevation = 4.dp
private val dimenHorizontalPagerHorizontal = 20.dp
private val dimenBottomHeight = 100.dp
private val dimenSearchHeight = 40.dp
private val dimenMinBottomHeight = 20.dp

private val bottomEnterAnimator = slideInVertically(animationSpec = tween(300),//动画时长1s
  initialOffsetY = {
    it//初始位置在负一屏的位置，也就是说初始位置我们看不到，动画动起来的时候会从负一屏位置滑动到屏幕位置
  })
private val bottomExitAnimator = slideOutVertically(animationSpec = tween(300),//动画时长1s
  targetOffsetY = {
    it//初始位置在负一屏的位置，也就是说初始位置我们看不到，动画动起来的时候会从负一屏位置滑动到屏幕位置
  })

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BrowserView(viewModel: BrowserViewModel) {
  val scaffoldState = rememberBottomSheetScaffoldState()
  
  BottomSheetScaffold(sheetContent = {
    Box(modifier = Modifier.fillMaxSize()) {
      Text(text = "我是控制项", modifier = Modifier.align(Alignment.Center))
    }
  },
  scaffoldState = scaffoldState) {
    Box(modifier = Modifier.fillMaxSize()) {
      Text(text = "我是内容", modifier = Modifier.align(Alignment.Center))
    }
  }
}