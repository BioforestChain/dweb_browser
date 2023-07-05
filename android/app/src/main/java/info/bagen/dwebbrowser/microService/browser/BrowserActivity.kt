package info.bagen.dwebbrowser.microService.browser

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import info.bagen.dwebbrowser.microService.browser.BrowserNMM.Companion.browserController
import info.bagen.dwebbrowser.ui.theme.DwebBrowserAppTheme
import info.bagen.dwebbrowser.util.FontDisplayUtil
import org.dweb_browser.browserUI.ui.browser.BrowserView
import org.dweb_browser.browserUI.ui.browser.LocalShowIme
import org.dweb_browser.browserUI.ui.browser.LocalShowSearchView
import org.dweb_browser.browserUI.ui.loading.LoadingView

class BrowserActivity : AppCompatActivity() {
  fun getContext() = this
  private var showSearchView = false
  private var mFontScale = 1f

  override fun getResources(): Resources {
    val resources = super.getResources()
    return FontDisplayUtil.getResources(this, resources, mFontScale)
  }

  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(FontDisplayUtil.attachBaseContext(newBase, mFontScale))
  }

  fun setFontScale(fontScale: Float) {
    mFontScale = fontScale
    FontDisplayUtil.recreate(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    browserController?.activity = this
    setContent {
      WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
        !isSystemInDarkTheme() // 设置状态栏颜色跟着主题走
      DwebBrowserAppTheme {
        val localShowSearchView = LocalShowSearchView.current
        LaunchedEffect(Unit) {
          snapshotFlow { localShowSearchView.value }.collect {
            showSearchView = it
          }
        }

        browserController?.apply {
          val localShowIme = LocalShowIme.current
          LaunchedEffect(Unit) {
            snapshotFlow { currentInsets.value }.collect {
              localShowIme.value = it.getInsets(WindowInsetsCompat.Type.ime()).bottom > 0
            }
          }
          effect(activity = this@BrowserActivity)
          Box(modifier = Modifier.background(Color.Black)) {
            BrowserView(viewModel = browserViewModel)
            LoadingView(showLoading)
          }
        }
      }
    }
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