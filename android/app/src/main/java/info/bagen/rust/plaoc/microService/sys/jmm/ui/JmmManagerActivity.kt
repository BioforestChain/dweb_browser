package info.bagen.rust.plaoc.microService.sys.jmm.ui

import android.content.Intent
import android.util.Log
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
            App.startActivity(JmmManagerActivity::class.java) { intent ->
                intent.action = ACTION_LAUNCH
                intent.`package` = "info.bagen.rust.plaoc"
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.putExtra(KEY_INSTALL_TYPE, type)
                jmmMetadata?.let { intent.putExtra(KEY_JMM_METADATA, it) }
            }
        }
    }

    val jmmManagerViewModel: JmmManagerViewModel = JmmManagerViewModel()//by viewModel()

    override fun initData() {
        App.jmmManagerActivity = this
        val screenType =
            intent.getSerializableExtra(KEY_INSTALL_TYPE)?.let { it as TYPE } ?: TYPE.MALL
        val jmmMetadata = intent.getSerializableExtra(KEY_JMM_METADATA)?.let { it as JmmMetadata }
        jmmManagerViewModel.handlerIntent(JmmIntent.SetTypeAndJmmMetaData(screenType, jmmMetadata))
    }

    @Composable
    override fun InitViews() {
        Log.e("lin.huang", "JmmManagerActivity::InitViews -> ${jmmManagerViewModel}")
        when (jmmManagerViewModel.uiState.currentType.value) {
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
            val jmmMetadata =
                intent.getSerializableExtra(KEY_JMM_METADATA)?.let { it as JmmMetadata }
            jmmManagerViewModel.handlerIntent(JmmIntent.SetTypeAndJmmMetaData(type, jmmMetadata))
        }
        val ttt = when (intent?.getIntExtra("lin.huang", -1)) {
            0 -> TYPE.MALL
            1 -> TYPE.INSTALL
            2 -> TYPE.UNINSTALL
            else -> null
        }
        ttt?.let { jmmManagerViewModel.handlerIntent(JmmIntent.SetTypeAndJmmMetaData(it, null)) }

    }

    override fun onStop() {
        super.onStop()
        finish() // 安装界面不需要一直存在，进入stop直接消失
    }

    override fun onDestroy() {
        App.jmmManagerActivity = null
        super.onDestroy()
    }
}
