package org.dweb_browser.baselineprofile

import android.view.KeyEvent
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * This test class generates a basic startup baseline profile for the target package.
 *
 * We recommend you start with this but add important user flows to the profile to improve their performance.
 * Refer to the [baseline profile documentation](https://d.android.com/topic/performance/baselineprofiles)
 * for more information.
 *
 * You can run the generator with the "Generate Baseline Profile" run configuration in Android Studio or
 * the equivalent `generateBaselineProfile` gradle task:
 * ```
 * ./gradlew :androidApp:generateReleaseBaselineProfile
 * ```
 * The run configuration runs the Gradle task and applies filtering to run only the generators.
 *
 * Check [documentation](https://d.android.com/topic/performance/benchmarking/macrobenchmark-instrumentation-args)
 * for more information about available instrumentation arguments.
 *
 * After you run the generator, you can verify the improvements running the [StartupBenchmarks] benchmark.
 *
 * When using this class to generate a baseline profile, only API 33+ or rooted API 28+ are supported.
 *
 * The minimum required version of androidx.benchmark to generate a baseline profile is 1.2.0.
 **/
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

  @get:Rule
  val rule = BaselineProfileRule()

  private var setup = false

  @Test
  fun generate() {
    // The application id for the running build variant is read from the instrumentation arguments.
    rule.collect(
      packageName = InstrumentationRegistry.getArguments().getString("targetAppId")
        ?: throw Exception("targetAppId not passed as instrumentation runner arg"),

      maxIterations = 1,
      // See: https://d.android.com/topic/performance/baselineprofiles/dex-layout-optimizations
      includeInStartupProfile = true
    ) {
      pressHome()
      startActivityAndWait()

      if (!setup) {
        device.waitForIdle();
        device.findObject(By.text("同意")).click()
        setup = true
      }

      device.waitForIdle()

      /// open webNmm
      device.bySelectorClick(By.desc("desk:web.browser.dweb"))

      /// 点击输入框
      device.bySelectorClick(By.text("Home Page|起始页".toPattern()))

      /// 清空输入框
      device.bySelectorClick(By.desc("Clear Input Text"))

      /// 输入测试地址
      device.pressKeyCodes("test.dwebdapp.com".map { it.toKeyCode() }.toIntArray())
      device.pressEnter()

      //#region 等待网页加载完成，点击COT下载
      device.wait(Until.findObject(By.text("ETHMeta")), 1000)
      /// 滚动网页
      val scrollable = UiScrollable(UiSelector().scrollable(true))
      scrollable.scrollBackward()
      /// 等待滚动结束
      device.performActionAndWait({}, Until.scrollFinished(Direction.DOWN), 1000)
      /// 点击COT
      device.bySelectorClick(By.text("COT"))
      /// 下载
      device.bySelectorClick(By.text("Install|安装".toPattern()), 2000)
      //#endregion

      /// 下载完成后，打开COT
      device.bySelectorClick(By.text("Open|打开".toPattern()), 300000)

      /// 强行等待两秒，等待COT打开完毕，
      device.waitForIdle(5000)
      val cotSelector = By.desc("taskbar:alphabfmeta.info.dweb")
      /// 先点击展开taskbar
      device.bySelectorClick(cotSelector)
      device.waitForIdle(2000)
      /// 双击将窗口变为浮动窗口
      device.bySelectorClick(cotSelector, clickType = ClickType.DOUBLE_CLICK)
      device.waitForIdle(3000)
      /// 长按
      device.bySelectorClick(cotSelector, clickType = ClickType.LONG_CLICK)
      device.waitForIdle(3000)
      /// 退出应用
      device.bySelectorClick(cotSelector)

      pressHome()
      // Check UiAutomator documentation for more information how to interact with the app.
      // https://d.android.com/training/testing/other-components/ui-automator
    }
  }
}

private enum class ClickType {
  CLICK,
  LONG_CLICK,
  DOUBLE_CLICK
}

private fun UiDevice.bySelectorClick(
  selector: BySelector,
  duration: Long = 1000,
  clickType: ClickType = ClickType.CLICK
) {
  wait(Until.hasObject(selector), TimeUnit.SECONDS.toMillis(duration))
  when (clickType) {
    ClickType.CLICK -> findObject(selector).click()
    ClickType.LONG_CLICK -> findObject(selector).longClick()
    ClickType.DOUBLE_CLICK -> {
      /// 模拟双击
      findObject(selector).click()
      Thread.sleep(50)
      findObject(selector).click()
    }
  }
}

fun Char.toKeyCode(): Int {
  return when (this) {
    'a', 'A' -> KeyEvent.KEYCODE_A
    'b', 'B' -> KeyEvent.KEYCODE_B
    'c', 'C' -> KeyEvent.KEYCODE_C
    'd', 'D' -> KeyEvent.KEYCODE_D
    'e', 'E' -> KeyEvent.KEYCODE_E
    'f', 'F' -> KeyEvent.KEYCODE_F
    'g', 'G' -> KeyEvent.KEYCODE_G
    'h', 'H' -> KeyEvent.KEYCODE_H
    'i', 'I' -> KeyEvent.KEYCODE_I
    'j', 'J' -> KeyEvent.KEYCODE_J
    'k', 'K' -> KeyEvent.KEYCODE_K
    'l', 'L' -> KeyEvent.KEYCODE_L
    'm', 'M' -> KeyEvent.KEYCODE_M
    'n', 'N' -> KeyEvent.KEYCODE_N
    'o', 'O' -> KeyEvent.KEYCODE_O
    'p', 'P' -> KeyEvent.KEYCODE_P
    'q', 'Q' -> KeyEvent.KEYCODE_Q
    'r', 'R' -> KeyEvent.KEYCODE_R
    's', 'S' -> KeyEvent.KEYCODE_S
    't', 'T' -> KeyEvent.KEYCODE_T
    'u', 'U' -> KeyEvent.KEYCODE_U
    'v', 'V' -> KeyEvent.KEYCODE_V
    'w', 'W' -> KeyEvent.KEYCODE_W
    'x', 'X' -> KeyEvent.KEYCODE_X
    'y', 'Y' -> KeyEvent.KEYCODE_Y
    'z', 'Z' -> KeyEvent.KEYCODE_Z
    '0' -> KeyEvent.KEYCODE_0
    '1' -> KeyEvent.KEYCODE_1
    '2' -> KeyEvent.KEYCODE_2
    '3' -> KeyEvent.KEYCODE_3
    '4' -> KeyEvent.KEYCODE_4
    '5' -> KeyEvent.KEYCODE_5
    '6' -> KeyEvent.KEYCODE_6
    '7' -> KeyEvent.KEYCODE_7
    '8' -> KeyEvent.KEYCODE_8
    '9' -> KeyEvent.KEYCODE_9
    ' ' -> KeyEvent.KEYCODE_SPACE
    ',' -> KeyEvent.KEYCODE_COMMA
    '.' -> KeyEvent.KEYCODE_PERIOD
    '@' -> KeyEvent.KEYCODE_AT
    '!' -> KeyEvent.KEYCODE_1  // Need to press shift for these symbols
    '#' -> KeyEvent.KEYCODE_3
    '$' -> KeyEvent.KEYCODE_4
    '%' -> KeyEvent.KEYCODE_5
    '^' -> KeyEvent.KEYCODE_6
    '&' -> KeyEvent.KEYCODE_7
    '*' -> KeyEvent.KEYCODE_8
    '(' -> KeyEvent.KEYCODE_9
    ')' -> KeyEvent.KEYCODE_0
    '-' -> KeyEvent.KEYCODE_MINUS
    '_' -> KeyEvent.KEYCODE_MINUS  // Need to press shift for these symbols
    '=' -> KeyEvent.KEYCODE_EQUALS
    '+' -> KeyEvent.KEYCODE_EQUALS  // Need to press shift for these symbols
    '[' -> KeyEvent.KEYCODE_LEFT_BRACKET
    ']' -> KeyEvent.KEYCODE_RIGHT_BRACKET
    '{' -> KeyEvent.KEYCODE_LEFT_BRACKET  // Need to press shift for these symbols
    '}' -> KeyEvent.KEYCODE_RIGHT_BRACKET  // Need to press shift for these symbols
    '\\' -> KeyEvent.KEYCODE_BACKSLASH
    '|' -> KeyEvent.KEYCODE_BACKSLASH  // Need to press shift for these symbols
    ';' -> KeyEvent.KEYCODE_SEMICOLON
    ':' -> KeyEvent.KEYCODE_SEMICOLON  // Need to press shift for these symbols
    '\'' -> KeyEvent.KEYCODE_APOSTROPHE
    '\"' -> KeyEvent.KEYCODE_APOSTROPHE  // Need to press shift for these symbols
    '/' -> KeyEvent.KEYCODE_SLASH
    '?' -> KeyEvent.KEYCODE_SLASH  // Need to press shift for these symbols
    else -> throw IllegalArgumentException("Unsupported char: $this")
  }
}
