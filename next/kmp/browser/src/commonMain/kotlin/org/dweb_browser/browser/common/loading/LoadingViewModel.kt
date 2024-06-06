package org.dweb_browser.browser.common.loading

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.helper.globalDefaultScope

object LoadingViewModel {
  private var isRunning: Boolean = false
  const val COUNT = 8

  private val whiteList = mutableListOf(
    Color(0xFFCCCCCC),
    Color(0xD6CCCCCC),
    Color(0xB8CCCCCC),
    Color(0x99CCCCCC),
    Color(0x7ACCCCCC),
    Color(0x5CCCCCCC),
    Color(0x3DCCCCCC),
    Color(0x1FCCCCCC)
  )

  private val blackList = mutableListOf(
    Color(0xFF000000),
    Color(0xD6000000),
    Color(0xB8000000),
    Color(0x99000000),
    Color(0x7A000000),
    Color(0x5C000000),
    Color(0x3D000000),
    Color(0x1F000000),
  )

  @Composable
  fun rememberColors(): List<Color> {
    return if (isSystemInDarkTheme()) whiteList else blackList
  }

  // val mTicker = mutableStateOf(0L)
  private val atomicIndex = atomic(0)
  var startIndex by mutableStateOf(atomicIndex.value)

  /**
   * 支付倒计时
   */
  fun startTimer() {
    isRunning = true
    globalDefaultScope.launch {
      startIndex = 0
      while (isRunning) {
        delay(100)
        startIndex = atomicIndex.addAndGet(1)
      }
    }
  }

  fun timerDestroy() {
    isRunning = false
    // mTicker.value = datetimeNow()
  }
}