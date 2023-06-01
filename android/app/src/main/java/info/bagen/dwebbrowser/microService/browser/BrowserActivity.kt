package info.bagen.dwebbrowser.microService.browser

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import info.bagen.dwebbrowser.microService.browser.BrowserNMM.Companion.browserController
import info.bagen.dwebbrowser.microService.sys.jmm.defaultJmmMetadata
import info.bagen.dwebbrowser.microService.sys.jmm.ui.JmmManagerActivity
import info.bagen.dwebbrowser.ui.browser.BrowserView
import info.bagen.dwebbrowser.ui.loading.LoadingView
import info.bagen.dwebbrowser.ui.theme.RustApplicationTheme

class BrowserActivity : AppCompatActivity() {
  fun getContext() = this

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    browserController?.activity = this
    setContent {
      WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
        !isSystemInDarkTheme() // 设置状态栏颜色跟着主题走
      RustApplicationTheme {
        browserController?.apply {
          effect(activity = this@BrowserActivity)
          Box(modifier = Modifier.background(Color.Black)) {
            BrowserView(viewModel = browserViewModel)
            LoadingView(showLoading)
          }
        }
        Box(modifier = Modifier.fillMaxSize().clickable { JmmManagerActivity.startActivity(defaultJmmMetadata) })
      }
    }
  }

  @Deprecated("Deprecated in Java")
  override fun onBackPressed() {
    if (browserController?.browserViewModel?.canMoveToBackground == true) {
      moveTaskToBack(false)
      return // 如果没有直接return，会导致重新打开app时，webview都是显示首页
    }
    super.onBackPressed()
  }

  override fun onStop() {
    super.onStop()
    browserController?.apply {
      if (showLoading.value) showLoading.value = false // 如果已经跳转了，这边直接改为隐藏
    }
  }

  override fun onDestroy() {
    // 退出APP关闭服务
    super.onDestroy()
    browserController?.activity = null
  }
}