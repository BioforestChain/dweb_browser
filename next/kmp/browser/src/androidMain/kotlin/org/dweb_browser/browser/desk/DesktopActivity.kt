package org.dweb_browser.browser.desk

import android.os.Build
import android.provider.Settings.Global
import android.webkit.WebView
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.core.view.WindowCompat
import com.qiniu.android.storage.UploadManager
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.helper.platform.PureViewController
import org.dweb_browser.helper.toJsonElement
import org.dweb_browser.sys.window.render.LocalWindowsImeVisible
import org.dweb_browser.sys.window.render.NativeBackHandler

@OptIn(ExperimentalLayoutApi::class)
class DesktopActivity : PureViewController() {

  init {
    onCreate {
      /// 禁止自适应布局，执行后，可以将我们的内容嵌入到状态栏和导航栏，但是会发现我们的界面呗状态栏和导航栏给覆盖了，这时候就需要systemUiController来改颜色
      WindowCompat.setDecorFitsSystemWindows(window, false)
      // lifecycleScope.launch { uploadDeviceInfo(this@DesktopActivity) } // 提交系统信息
    }

    addContent {
      NativeBackHandler {
        moveTaskToBack(true) // 将界面移动到后台，避免重新点击又跑SplashActivity
      }
    }

    DesktopViewControllerCore(this)

    addContent {
      val imeVisible = LocalWindowsImeVisible.current
      val density = LocalDensity.current
      val ime =
        androidx.compose.foundation.layout.WindowInsets.imeAnimationTarget // 直接使用ime，数据不稳定，会变化，改为imeAnimationTarget就是固定值
      LaunchedEffect(ime) {
        window.decorView.viewTreeObserver.addOnGlobalLayoutListener {
          /*val rect = android.graphics.Rect()
          window.decorView.getWindowVisibleDisplayFrame(rect)
          val screenHeight = window.decorView.rootView.height
          val screenDifference = screenHeight - rect.bottom
          val visible =  screenDifference > screenHeight / 3
          imeVisible.value = visible*/
          imeVisible.value = ime.getBottom(density) != 0
        }
      }
    }
  }
}

/**
 * 获取设备一些简单信息
 */
private val UPTOKEN_Z0 =
  "vO3IeF4GypmPpjMnkHcZZo67hHERojsvLikJxzj5:3mGwo1JM8m5bVGCuv5dr_tnYcag=:eyJzY29wZSI6ImphY2tpZS15ZWxsb3c6bW9kZWxfIiwiZGVhZGxpbmUiOjE4MDQ4ODUwOTMsImlzUHJlZml4YWxTY29wZSI6MX0="

private fun uploadDeviceInfo(activity: DesktopActivity) {
  val uploadManager = UploadManager()
  val map = mutableMapOf<String, String>()
  map["MANUFACTURER"] = Build.MANUFACTURER
  map["DEVICE"] = Build.DEVICE
  map["DEVICE-NAME"] = Global.getString(activity.contentResolver, Global.DEVICE_NAME)
  map["MODEL"] = Build.MODEL
  map["HARDWARE"] = Build.HARDWARE
  map["Android Version"] = Build.VERSION.RELEASE
  if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
    map["SOC_MANUFACTURER"] = Build.SOC_MANUFACTURER
    map["SOC_MODEL"] = Build.SOC_MODEL
    map["SKU"] = Build.SKU
  }
  WebView.getCurrentWebViewPackage()?.let { packageName ->
    map["webview_package"] = packageName.packageName
    map["webview_version"] = packageName.versionName
  }
  uploadManager.put(
    map.toJsonElement().toString().toByteArray(),
    "model_baidu/system_${Build.MANUFACTURER}_${datetimeNow()}.txt",
    UPTOKEN_Z0,
    { _, info, _ ->
      if (info?.isOK == true) {
        println("Push Success")
      } else {
        println("Push Fail ${info?.error}")
      }
    },
    null
  )
}