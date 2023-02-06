package info.bagen.rust.plaoc

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import info.bagen.libappmgr.R
import info.bagen.libappmgr.ui.splash.SplashPrivacyDialog
import info.bagen.libappmgr.utils.getBoolean
import info.bagen.libappmgr.utils.saveBoolean
import info.bagen.rust.plaoc.microService.startBootNMM
import info.bagen.rust.plaoc.ui.theme.RustApplicationTheme
import info.bagen.rust.plaoc.webView.DWebViewActivity
import info.bagen.rust.plaoc.webView.openDWebWindow

class SplashActivity : AppCompatActivity() {
    private val keyAppFirstLoad = "App_First_Load"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    override fun onResume() {
        super.onResume()
        val first = this.getBoolean(keyAppFirstLoad, true)
        if (first) {
            setContent {
                RustApplicationTheme {
                    SplashMainView()
                    SplashPrivacyDialog(
                        openHome = {  openHomeActivity() },
                        openWebView = { url -> openDWebWindow(this, url) },
                        closeApp = { finish() }
                    )
                }
                this.saveBoolean(keyAppFirstLoad, false)
            }
        } else {
            startBootNMM()
            this.saveBoolean(keyAppFirstLoad, false)
            finish()
        }
    }

}

fun openHomeActivity(): Boolean {
    val intent = Intent(App.appContext.applicationContext, MainActivity::class.java).apply {
        addFlags(FLAG_ACTIVITY_NEW_TASK)
    }
    App.appContext.startActivity(intent)
    return true
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun SplashMainView() {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val gradient = listOf(
                Color(0xFF71D78E), Color(0xFF548FE3)
            )
            Text(
                text = stringResource(id = R.string.app_name),
                modifier = Modifier.align(Alignment.BottomCenter),
                style = TextStyle(
                    brush = Brush.linearGradient(gradient), fontSize = 50.sp
                )
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}
