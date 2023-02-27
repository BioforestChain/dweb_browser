package info.bagen.rust.plaoc.microService.sys.mwebview

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import info.bagen.rust.plaoc.microService.helper.Mmid
import info.bagen.rust.plaoc.ui.theme.RustApplicationTheme

class MutilWebViewActivity : AppCompatActivity() {
    //    val dWebBrowserModel
    fun openWebView(url: String): String {
        return url
    }

    fun closeWebView(webviewId: String): Boolean {
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val remoteMmid = intent.getStringExtra("mmid") as Mmid

        MultiWebViewNMM.activityMap[remoteMmid]?.also { taskPo ->
            taskPo.resolve(this)
        } ?: return finish()

        setContent {
            RustApplicationTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.primary)
                ) {
//                    MultiDWebView(dWebBrowserModel = dWebBrowserModel)
                }
            }
        }
    }
}