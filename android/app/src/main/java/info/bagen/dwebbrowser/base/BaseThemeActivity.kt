package info.bagen.dwebbrowser.base

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.core.view.WindowCompat
import info.bagen.dwebbrowser.ui.theme.DwebBrowserAppTheme

abstract class BaseThemeActivity : org.dweb_browser.dwebview.base.BaseActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initData()
    setContent {
      DwebBrowserAppTheme {
        WindowCompat.getInsetsController(window, window.decorView)
          .isAppearanceLightStatusBars = !isSystemInDarkTheme() // 设置状态栏颜色跟着主题走
        InitViews()
      }
    }
  }

  open fun initData() {} // 初始化数据，或者注册监听

  @Composable
  open fun InitViews() {
  }// 填充Compose布局
}