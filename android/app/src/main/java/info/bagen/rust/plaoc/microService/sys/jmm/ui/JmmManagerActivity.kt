package info.bagen.rust.plaoc.microService.sys.jmm.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.base.BaseActivity
import info.bagen.rust.plaoc.microService.sys.jmm.JmmMetadata

class JmmManagerActivity : BaseActivity() {

  companion object {
    const val ACTION_LAUNCH = "info.bagen.dwebbrowser.openjmm"
    const val KEY_INSTALL_TYPE = "key_install_type"
    const val KEY_JMM_METADATA = "key_jmm_meta_data"

    fun startActivity(jmmMetadata: JmmMetadata? = null, type: TYPE = TYPE.MALL) {
      val intent = Intent(App.appContext, JmmManagerActivity::class.java).apply {
        action = ACTION_LAUNCH
        `package` = "info.bagen.rust.plaoc"
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
        putExtra(KEY_INSTALL_TYPE, type)
        jmmMetadata?.let { putExtra(KEY_JMM_METADATA, jmmMetadata) }
      }
      App.appContext.startActivity(intent)
    }
  }

  private var screenType : TYPE = TYPE.MALL
  private var jmmMetadata: JmmMetadata? = null
  val jmmManagerViewModel : JmmManagerViewModel = JmmManagerViewModel()//by viewModel()

  override fun initData() {
    App.jmmManagerActivity = this
    screenType = intent.getSerializableExtra(KEY_INSTALL_TYPE) as TYPE
    jmmMetadata = intent.getSerializableExtra(KEY_JMM_METADATA) as JmmMetadata
    jmmManagerViewModel.handlerIntent(JmmIntent.SetTypeAndJmmMetaData(screenType, jmmMetadata))
  }

  @Composable
  override fun InitViews() {
    when (screenType) {
      TYPE.MALL -> MALLBrowserView(jmmManagerViewModel) { url, name ->
        // DwebBrowserUtil.INSTANCE.mBinderService?.invokeDownloadAndSaveZip(url, name)
        jmmManagerViewModel.handlerIntent(JmmIntent.DownLoadAndSave)
        // this@JmmManagerActivity.finish() // 点击下载后可以直接关闭当前界面，或者同步更新按钮的状态
      }
      TYPE.INSTALL -> {
        InstallBrowserView(jmmManagerViewModel)
      }
      TYPE.UNINSTALL -> {
        UninstallBrowserView(jmmManagerViewModel)
      }
    }
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    // 该方法被执行，说明当前的activity是打开状态的，这边需要看下是否修改显示
    (intent?.getSerializableExtra(KEY_INSTALL_TYPE))?.let {
      val type = it as TYPE
      screenType = type
      jmmManagerViewModel.handlerIntent(JmmIntent.SetTypeAndJmmMetaData(type, null))
    }
  }

  override fun onPause() {
    super.onPause()
    finish() // 安装界面不需要一直存在，进入pause直接消失
  }

  override fun onDestroy() {
    App.jmmManagerActivity = null
    super.onDestroy()
  }
}
