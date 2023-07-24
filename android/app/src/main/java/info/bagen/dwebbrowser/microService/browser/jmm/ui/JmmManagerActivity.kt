package info.bagen.dwebbrowser.microService.browser.jmm.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.base.BaseActivity
import org.dweb_browser.helper.AppMetaData
import info.bagen.dwebbrowser.microService.browser.jmm.JmmNMM

class JmmManagerActivity : BaseActivity() {

  companion object {
    const val ACTION_LAUNCH = "info.bagen.dwebbrowser.openjmm"
    const val KEY_JMM_METADATA = "key_jmm_meta_data"

    fun startActivity(dataMetadata: AppMetaData) {
      App.startActivity(JmmManagerActivity::class.java) { intent ->
        intent.action = ACTION_LAUNCH
        intent.`package` = "info.bagen.dwebbrowser"
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
        intent.putExtra(KEY_JMM_METADATA, dataMetadata)
      }
    }
  }

  private lateinit var jmmManagerViewModel: JmmManagerViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    transparentNavigatorBar()
    transparentStatusBar()
    super.onCreate(savedInstanceState)

  }

  override fun initData() {
    intent.getSerializableExtra(KEY_JMM_METADATA)?.let {
      val appMetaData = it as AppMetaData
      jmmManagerViewModel = JmmManagerViewModel(appMetaData, JmmNMM.jmmController)
    }
  }

  @Composable
  override fun InitViews() {
    MALLBrowserView(jmmManagerViewModel) { finish() }
  }

  override fun onStop() {
    super.onStop()
    finish() // 安装界面不需要一直存在，进入stop直接消失
  }

  override fun onDestroy() {
    jmmManagerViewModel.handlerIntent(JmmIntent.DestroyActivity)
    super.onDestroy()
  }

  private fun transparentStatusBar() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
      window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
      val option = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
      val vis = window.decorView.systemUiVisibility
      window.decorView.systemUiVisibility = option or vis
      window.statusBarColor = Color.Transparent.value.toInt()
    } else {
      window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
    }
  }

  private fun transparentNavigatorBar() {

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      window.isNavigationBarContrastEnforced = false
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      window.navigationBarColor = Color.Transparent.value.toInt()
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      if (window.attributes.flags and WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION === 0) {
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
      }
    }
    val decorView = window.decorView
    val vis = decorView.systemUiVisibility
    val option =
      View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    decorView.systemUiVisibility = vis or option
  }
}
