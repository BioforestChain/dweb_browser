package info.bagen.rust.plaoc.microService.sys.mwebview

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import info.bagen.rust.plaoc.ui.theme.RustApplicationTheme

open class MutilWebViewActivity : AppCompatActivity() {


    private var remoteMmid by mutableStateOf("")
    private fun upsetRemoteMmid() {

        remoteMmid = intent.getStringExtra("mmid")
            ?: return finish()
    }

    override fun onResume() {
        super.onResume()
        upsetRemoteMmid()
    }

    override fun onRestart() {
        super.onRestart()
        upsetRemoteMmid()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        upsetRemoteMmid()


        setContent {
            RustApplicationTheme {
                val wc by remember {
                    mutableStateOf(
                        MultiWebViewNMM.controllerMap[remoteMmid]
                            ?: throw Exception("no found controller by mmid:$remoteMmid")
                    )
                }

                val viewItem = wc.webViewList?.lastOrNull()
                if (viewItem != null) key(viewItem.webviewId) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                viewItem.dWebView
                            },
                            modifier = Modifier
                                .fillMaxSize(),
                        )
                    }
                }

            }

        }
    }

}

class MutilWebViewPlaceholder1Activity : MutilWebViewActivity()
class MutilWebViewPlaceholder2Activity : MutilWebViewActivity()
class MutilWebViewPlaceholder3Activity : MutilWebViewActivity()
class MutilWebViewPlaceholder4Activity : MutilWebViewActivity()
class MutilWebViewPlaceholder5Activity : MutilWebViewActivity()