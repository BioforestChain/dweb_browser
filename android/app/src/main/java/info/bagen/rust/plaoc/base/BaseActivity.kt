package info.bagen.rust.plaoc.base

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import info.bagen.rust.plaoc.ui.theme.RustApplicationTheme

abstract class BaseActivity: AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initData()
    setContent {
      RustApplicationTheme {
        ViewCompat.getWindowInsetsController(LocalView.current)?.isAppearanceLightStatusBars =
          !isSystemInDarkTheme() // 设置状态栏颜色跟着主题走
        InitViews()
      }
    }
  }

  abstract fun initData() // 初始化数据，或者注册监听

  @Composable
  abstract fun InitViews() // 填充Compose布局
}