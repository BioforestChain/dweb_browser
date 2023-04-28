package info.bagen.dwebbrowser.ui.browser.android

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

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

  BottomSheetScaffold(
    sheetContent = {
      Box(modifier = Modifier.fillMaxSize()) {
        Text(text = "我是控制项", modifier = Modifier.align(Alignment.Center))
      }
    },
    scaffoldState = scaffoldState
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      Text(text = "我是内容", modifier = Modifier.align(Alignment.Center))
    }
  }
}