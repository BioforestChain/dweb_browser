package info.bagen.dwebbrowser.microService.browser.web

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import info.bagen.dwebbrowser.base.BaseActivity
import info.bagen.dwebbrowser.ui.theme.DwebBrowserAppTheme
import org.dweb_browser.browserUI.ui.browser.BrowserView
import org.dweb_browser.browserUI.ui.browser.LocalShowIme
import org.dweb_browser.browserUI.ui.browser.LocalShowSearchView
import org.dweb_browser.browserUI.ui.loading.LoadingView

class BrowserActivity : BaseActivity() {

  private var controller: BrowserController? = null
  private fun bindController(sessionId: String?): BrowserController {
    /// 解除上一个 controller的activity绑定
    controller?.activity = null

    return BrowserNMM.controllers[sessionId]?.also { browserController ->
      browserController.activity = this
      controller = browserController
    } ?: throw Exception("no found controller by sessionId: $sessionId")
  }

  fun getContext() = this
  private var showSearchView = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val browserController = bindController(intent.getStringExtra("sessionId"))
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

        browserController.apply {
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
    showSearchView(intent)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    showSearchView(intent)
  }

  private fun showSearchView(intent: Intent) {
    controller?.browserViewModel?.apply {
      val search = intent.getStringExtra("search") ?: ""
      this.search.value = search
    }
  }
}