package com.example.benchmark

import android.util.Log
import android.view.View
import android.webkit.WebView
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.web.sugar.Web
import androidx.test.espresso.web.webdriver.DriverAtoms
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import org.hamcrest.Matcher
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
 *
 * TODO 运行下面的 startup 前，需要进入 androidApp 的 build.gradle 修改 release 的 signingConfig 为 debug
 * TODO 或者创建 next/kmp/app/androidApp/key.properties 文件
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

    Web.onWebView(ViewMatchers.withTagKey(1)).withElement(
      DriverAtoms.findElement(
        Locator.XPATH,
        "//.app-name[contains(text(),'浏览器')]"
      )
    )
      .perform(DriverAtoms.webClick())
    device.waitForIdle()
  }
}

/**
 * 产生 baseline profile 文件
 * 路径 build/outputs/connected_android_test_additional_output/benchmark/connected/AVD/BaselineProfileGenerator_startup-baseline-prof-2023-02-14-09-39-28.txt
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
  @get:Rule
  val baselineProfileRule = BaselineProfileRule()

  var setup = false

  @Test
  fun startup() = baselineProfileRule.collect(packageName = "info.bagen.dwebbrowser") {
    pressHome()
    startActivityAndWait()

    if (!setup) {
      device.waitForIdle();
      device.findObject(By.text("同意")).click()
      setup = true
    }

    device.waitForIdle()
    Log.d("WebView", "try click webview")
    device.click(450, 550)
    device.waitForIdle()
    device.drag(445, 240, 560, 1150, 50)

    Thread.sleep(1000)

    pressHome()
    device.swipe(1000, 800, 1000, 800, 50)
    Thread.sleep(100)
    device.click(1000, 800)

    // 找到 WebView 并执行操作
    if (false) {
//      device.findObject(By.clazz(WebView::class.java) ).click()

      onView(ViewMatchers.withId(1)).perform(object : ViewAction {
        override fun getConstraints(): Matcher<View> =
          isAssignableFrom(WebView::class.java)

        override fun getDescription(): String = "点击 WebView 内的 '浏览器' 元素"

        override fun perform(uiController: UiController?, view: View?) {
          val webView = view as WebView

          webView.evaluateJavascript(
            """
            const ele = document.querySelector("#app > div > div > main > div > div > div:nth-child(3) > div > div > div.app-name.ios-ani")
            ele.click()
            ele.innerText
          """.trimIndent()
          )
          { result ->
            Log.d("WebView", "JavaScript 结果: $result")
          }
        }
      })
    }

    if (false) {

      val webView = Web.onWebView(ViewMatchers.withId(1)).forceJavascriptEnabled()
      webView.withElement(
        DriverAtoms.findElement(
          Locator.XPATH,
          "/html/body/div/div/div/main/div/div/div[3]/div/div/div[2]"
        )
      )
        .perform(DriverAtoms.webClick())
    }


    // 验证 WebView 是否获取到正确实例，并打印当前 URL
//    val webViewInstance =
//      device.findObjects(By.clazz(WebView::class.java)).mapNotNull { uiObject ->
//        try {
//          val field = uiObject.javaClass.getDeclaredField("mCacheNode")
//          field.isAccessible = true
//          field.get(uiObject) as WebView
//        } catch (e: NoSuchFieldException) {
//          e.printStackTrace()
//          null
//        } catch (e: IllegalAccessException) {
//          e.printStackTrace()
//          null
//        }
//      }
//        .firstOrNull { it.tag == 1 }
//
//    webViewInstance?.evaluateJavascript(
//      """
//      const ele = document.querySelector("#app > div > div > main > div > div > div:nth-child(3) > div > div > div.app-name.ios-ani")
//      ele.click()
//      ele.innerText
//    """.trimIndent()
//    ) {
//      println("clicked $it")
//    }

//    DriverAtoms.webClick()
//        val jsGetUrl = DriverAtoms.script("return window.location.href;")
//        val urlAtom: Atom<String> = DriverAtoms.script("return window.location.href;")
//
//    val browserNmmXpath = "//*[@id='app']/div/div/main/div/div/div[3]/div/div/div[2]"
//
//    val webDoc =
//      webView.check(webContent(elementByXPath(browserNmmXpath, withTextContent("浏览器"))))
//    println("QAQ webDoc=$webDoc")
//
//    webDoc.withElement(
//      DriverAtoms.findElement(
//        Locator.XPATH,
//        browserNmmXpath
//      )
//    )
//      .perform(DriverAtoms.webClick())

//    device.pressBack()
  }

// 通过反射查找 WebView 实例
//  private fun findWebViewInstance(device: UiDevice): WebView? {
//    val uiObject: UiObject = device.findObjects(By.clazz("android.webkit.WebView"))
//    return try {
//      val field = uiObject.javaClass.getDeclaredField("mRealView")
//      field.isAccessible = true
//      field.get(uiObject) as WebView
//    } catch (e: NoSuchFieldException) {
//      e.printStackTrace()
//      null
//    } catch (e: IllegalAccessException) {
//      e.printStackTrace()
//      null
//    }
//  }
}
