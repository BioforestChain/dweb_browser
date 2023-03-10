package info.bagen.rust.plaoc.microService.sys.jmm.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.base.BaseActivity
import info.bagen.rust.plaoc.microService.sys.jmm.JmmMetadata
import info.bagen.rust.plaoc.microService.sys.jmm.defaultJmmMetadata

class JmmManagerActivity : BaseActivity() {

  companion object {
    const val ACTION_LAUNCH = "info.bagen.dwebbrowser.openjmm"
    const val KEY_JMM_METADATA = "key_jmm_meta_data"

    fun startActivity(jmmMetadata: JmmMetadata) {
      App.startActivity(JmmManagerActivity::class.java) { intent ->
        intent.action = ACTION_LAUNCH
        intent.`package` = "info.bagen.rust.plaoc"
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
        intent.putExtra(KEY_JMM_METADATA, jmmMetadata)
      }
    }
  }

  private lateinit var jmmManagerViewModel: JmmManagerViewModel

  override fun initData() {
    val jmmMetadata = intent.getSerializableExtra(KEY_JMM_METADATA)?.let { it as JmmMetadata }
      ?: defaultJmmMetadata
    jmmManagerViewModel = JmmManagerViewModel(jmmMetadata)
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
