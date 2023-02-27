package info.bagen.rust.plaoc.microService.webview

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.*
import info.bagen.rust.plaoc.ui.theme.RustApplicationTheme

class MutilWebViewActivity : AppCompatActivity() {
//    val dWebBrowserModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // intent.getStringExtra("mmid")
        setContent {
            RustApplicationTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colors.primary)
                ) {
//                    MultiDWebBrowserView(dWebBrowserModel = dWebBrowserModel)
                }
            }
        }
    }
}