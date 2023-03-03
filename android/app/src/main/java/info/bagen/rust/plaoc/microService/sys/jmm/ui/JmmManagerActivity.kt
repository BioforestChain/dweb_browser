package info.bagen.rust.plaoc.microService.sys.jmm.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import info.bagen.rust.plaoc.App
import info.bagen.rust.plaoc.base.BaseActivity
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.microService.helper.PromiseOut
import info.bagen.rust.plaoc.microService.sys.jmm.JmmMetadata
import info.bagen.rust.plaoc.microService.sys.jmm.defaultJmmMetadata

class JmmManagerActivity : BaseActivity() {

    companion object {
        const val ACTION_LAUNCH = "info.bagen.dwebbrowser.openjmm"
        const val KEY_INSTALL_TYPE = "key_install_type"
        const val KEY_JMM_METADATA = "key_jmm_meta_data"
        val downLoadStatus = mutableMapOf<Mmid, PromiseOut<DownLoadStatus>>()

        fun startActivity(jmmMetadata: JmmMetadata) {
            App.startActivity(JmmManagerActivity::class.java) { intent ->
                intent.action = ACTION_LAUNCH
                intent.`package` = "info.bagen.rust.plaoc"
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.putExtra(KEY_JMM_METADATA, jmmMetadata)
            }
        }
    }

    val jmmManagerViewModel: JmmManagerViewModel = JmmManagerViewModel()//by viewModel()

    override fun initData() {
        App.jmmManagerActivity = this
        val jmmMetadata = intent.getSerializableExtra(KEY_JMM_METADATA)?.let { it as JmmMetadata }
            ?: defaultJmmMetadata
        // 初始化下载状态
        downLoadStatus.getOrPut(jmmMetadata.id) {
            PromiseOut()
        }
        jmmManagerViewModel.handlerIntent(JmmIntent.SetTypeAndJmmMetaData(jmmMetadata))
    }

    @Composable
    override fun InitViews() {
        MALLBrowserView(jmmManagerViewModel)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // 该方法被执行，说明当前的activity是打开状态的，这边需要看下是否修改显示
        (intent?.getSerializableExtra(KEY_INSTALL_TYPE))?.let {
            val jmmMetadata =
                intent.getSerializableExtra(KEY_JMM_METADATA)?.let { mData -> mData as JmmMetadata }
                    ?: defaultJmmMetadata
            jmmManagerViewModel.handlerIntent(JmmIntent.SetTypeAndJmmMetaData(jmmMetadata))
        }
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
