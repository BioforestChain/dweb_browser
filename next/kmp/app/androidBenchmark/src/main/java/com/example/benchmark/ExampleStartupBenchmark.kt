package com.example.benchmark

import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This is an example startup benchmark.
 *
 * It navigates to the device's home screen, and launches the default activity.
 *
 * Before running this benchmark:
 * 1) switch your app's active build variant in the Studio (affects Studio runs only)
 * 2) add `<profileable android:shell="true" />` to your app's manifest, within the `<application>` tag
 *
 * Run this benchmark from Studio to see startup measurements, and captured system traces
 * for investigating your app's performance.
 */
@RunWith(AndroidJUnit4::class)
class ExampleStartupBenchmark {
  @get:Rule
  val benchmarkRule = MacrobenchmarkRule()

  @Test
  fun startup() = benchmarkRule.measureRepeated(
    packageName = "info.bagen.dwebbrowser",
    metrics = listOf(StartupTimingMetric()),
    iterations = 5,
    startupMode = StartupMode.COLD
  ) {
    pressHome()
    startActivityAndWait()

    device.waitForIdle()
    device.findObject(By.text("同意")).click()
//    Espresso.onView(ViewMatchers.withText("同意"))
//      .perform(ViewActions.click())
    device.waitForIdle()

//    Web.onWebView(ViewMatchers.withTagKey(1)).withElement(
//      DriverAtoms.findElement(
//        Locator.XPATH,
//        "//.app-name[contains(text(),'浏览器')]"
//      )
//    )
//      .perform(DriverAtoms.webClick())
//    device.waitForIdle()
  }
}

/**
 * 产生 baseline profile 文件
 * TODO 运行下面的 startup 前，需要进入 androidApp 的 build.gradle 修改 release 的 signingConfig 为 debug
 * 路径 build/outputs/connected_android_test_additional_output/benchmark/connected/AVD/BaselineProfileGenerator_startup-baseline-prof-2023-02-14-09-39-28.txt
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
  @get:Rule
  val baselineProfileRule = BaselineProfileRule()

  @Test
  fun startup() = baselineProfileRule.collect(packageName = "info.bagen.dwebbrowser") {
    pressHome()
    startActivityAndWait()

    device.waitForIdle()
    device.findObject(By.text("同意")).click()
    device.waitForIdle()
  }
}
