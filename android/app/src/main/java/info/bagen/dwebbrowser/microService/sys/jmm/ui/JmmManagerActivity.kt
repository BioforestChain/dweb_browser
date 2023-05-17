package info.bagen.dwebbrowser.microService.sys.jmm.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.base.BaseActivity
import info.bagen.dwebbrowser.microService.sys.jmm.JmmMetadata
import info.bagen.dwebbrowser.microService.sys.jmm.JmmNMM

class JmmManagerActivity : BaseActivity() {

  companion object {
    const val ACTION_LAUNCH = "info.bagen.dwebbrowser.openjmm"
    const val KEY_JMM_METADATA = "key_jmm_meta_data"

    fun startActivity(jmmMetadata: JmmMetadata) {
      App.startActivity(JmmManagerActivity::class.java) { intent ->
        intent.action = ACTION_LAUNCH
        intent.`package` = "info.bagen.dwebbrowser"
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
        intent.putExtra(KEY_JMM_METADATA, jmmMetadata)
      }
    }
  }

  private lateinit var jmmManagerViewModel: JmmManagerViewModel

  override fun initData() {
    intent.getSerializableExtra(KEY_JMM_METADATA)?.let {
      val jmmMetadata = it as JmmMetadata
      jmmManagerViewModel = JmmManagerViewModel(jmmMetadata, JmmNMM.jmmController)
    }
  }

  @Composable
  override fun InitViews() {
    MALLBrowserView(jmmManagerViewModel)
  }

  override fun onStop() {
    super.onStop()
    finish() // 安装界面不需要一直存在，进入stop直接消失
  }

  override fun onDestroy() {
    jmmManagerViewModel.handlerIntent(JmmIntent.DestroyActivity)
    super.onDestroy()
  }
}
