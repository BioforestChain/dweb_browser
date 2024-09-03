package org.dweb_browser.helper.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

sealed interface FixRefreshMode {
  companion object {
    data class FixRefreshTimerMode(val id: Long, val time: Long, val delay: Long = 0) :
      FixRefreshMode

    data class FixRefreshOnceMode(val id: Long) : FixRefreshMode
    data object FixRefreshInfiniteMode : FixRefreshMode
  }
}

/**
 * 可以用在 IOS平台上，popup居然回因为外部scale导致位置重新计算
 * 可以用在非全屏的PVC旋转后，需要进行compose强制更新渲染
 */
@Composable
fun FixIosLayerRefresh(mode: FixRefreshMode = FixRefreshMode.Companion.FixRefreshInfiniteMode) {
  val play = when (mode) {
    FixRefreshMode.Companion.FixRefreshInfiniteMode -> true
    is FixRefreshMode.Companion.FixRefreshTimerMode -> {
      var play by remember(mode) {
        mutableStateOf(
          when {
            mode.delay > 0 -> false
            else -> true
          }
        )
      }
      LaunchedEffect(mode.time, mode.id) {
        if (mode.delay > 0) {
          delay(mode.delay)
        }
        play = true
        delay(mode.time)
        play = false
      }
      play
    }

    is FixRefreshMode.Companion.FixRefreshOnceMode -> {
      var play by remember(mode.id) { mutableStateOf(true) }
      LaunchedEffect(play) {
        play = false
      }
      play
    }
  }
  /// 所以这里交叉切换，来强行让每一帧都在刷新位置
  if (play) {
    FixIosLayerRender(1f)
    FixIosLayerRender(2f)
  }
}

@Composable
private fun FixIosLayerRender(initScale: Float) {
  var scale by remember { mutableStateOf(initScale) }
  LaunchedEffect(scale) {
    scale = when (scale) {
      1f -> 2f
      else -> 1f
    }
  }
  /// 发现每次切回1的时候，它就正常一次？
  Box(Modifier.scale(scale).size(1.dp, 1.dp))
}