package org.dweb_browser.browser.common.loading

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.dweb_browser.helper.ioAsyncExceptionHandler
import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
object LoadingViewModel {
  private val ioAsyncScope = MainScope() + ioAsyncExceptionHandler
  private var isRunning: Boolean = false

  private val white1 = Color(0xFFCCCCCC)
  private val white2 = Color(0xD6CCCCCC)
  private val white3 = Color(0xB8CCCCCC)
  private val white4 = Color(0x99CCCCCC)
  private val white5 = Color(0x7ACCCCCC)
  private val white6 = Color(0x5CCCCCCC)
  private val white7 = Color(0x3DCCCCCC)
  private val white8 = Color(0x1FCCCCCC)

  private val black1 = Color(0xFF000000)
  private val black2 = Color(0xD6000000)
  private val black3 = Color(0xB8000000)
  private val black4 = Color(0x99000000)
  private val black5 = Color(0x7A000000)
  private val black6 = Color(0x5C000000)
  private val black7 = Color(0x3D000000)
  private val black8 = Color(0x1F000000)

  val mColor = mutableListOf(
    white1,
    white2,
    white3,
    white4,
    white5,
    white6,
    white7,
    white8,
  )

  fun setBackground(whiteBackground: Boolean) {
    if (whiteBackground) {
      mColor[0] = black1
      mColor[1] = black2
      mColor[2] = black3
      mColor[3] = black4
      mColor[4] = black5
      mColor[5] = black6
      mColor[6] = black7
      mColor[7] = black8
    } else {
      mColor[0] = white1
      mColor[1] = white2
      mColor[2] = white3
      mColor[3] = white4
      mColor[4] = white5
      mColor[5] = white6
      mColor[6] = white7
      mColor[7] = white8
    }
  }

  // val mTicker = mutableStateOf(0L)
  private val atomicIndex = atomic(0L)
  val mCount = mutableStateOf(atomicIndex.value)

  /**
   * 支付倒计时
   */
  fun startTimer() {
    isRunning = true
    ioAsyncScope.launch {
      while (isRunning) {
        delay(100)
        mCount.value = atomicIndex.addAndGet(1)
        /*val data = mColor.removeLast()
        mColor.add(0, data)
        mTicker.value = datetimeNow()*/
      }
    }
  }

  fun timerDestroy() {
    isRunning = false
    // mTicker.value = datetimeNow()
  }
}