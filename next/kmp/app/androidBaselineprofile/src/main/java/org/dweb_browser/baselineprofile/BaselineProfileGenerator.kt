package org.dweb_browser.baselineprofile

import android.view.KeyEvent
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
      device.click(450, 550)
      device.waitForIdle()

      /// goto web page
      Thread.sleep(100)
      device.click(450, 1080)
      device.waitForIdle()
      Thread.sleep(100)
      device.pressKeyCodes("dwebdapp.com".map { it.toKeyCode() }.toIntArray())
      device.pressEnter()
      Thread.sleep(8000)

      /// install app
      device.click(700, 970)
      device.waitForIdle()
      Thread.sleep(1500)
      device.swipe(560, 1260, 560, 200, 20)
      device.waitForIdle()
      Thread.sleep(800)
      device.click(560, 2170)
      Thread.sleep(12000)
      device.waitForIdle()

      /// open app
      device.waitForIdle()
      device.click(560, 2170)
      device.waitForIdle()
      Thread.sleep(2000)

      /// app max->float
      device.click(935, 2170)
      device.waitForIdle()
      Thread.sleep(200)

      /// kill app
      device.swipe(1000, 800, 1000, 800, 100)
      Thread.sleep(100)
      device.click(1000, 800)
      Thread.sleep(200)

      /// move window
      device.click(445, 240)
      device.drag(445, 240, 560, 1150, 50)
      Thread.sleep(1000)

      /// kill webNmm
      device.swipe(1000, 800, 1000, 800, 100)
      Thread.sleep(100)
      device.click(1000, 800)
      Thread.sleep(200)

      // pressHome()
      // Check UiAutomator documentation for more information how to interact with the app.
      // https://d.android.com/training/testing/other-components/ui-automator
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
