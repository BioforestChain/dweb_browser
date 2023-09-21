package info.bagen.dwebbrowser

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.browserUI.ui.splash.SplashPrivacyDialog
import org.dweb_browser.browserUI.ui.view.CommonWebView
import org.dweb_browser.browserUI.ui.view.LocalCommonUrl
import org.dweb_browser.browserUI.util.KEY_ENABLE_AGREEMENT
import org.dweb_browser.browserUI.util.getBoolean
import org.dweb_browser.browserUI.util.saveBoolean
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.compose.theme.DwebBrowserAppTheme
import org.dweb_browser.helper.ioAsyncExceptionHandler
import kotlin.system.exitProcess

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
  private var mKeepOnAtomicBool = java.util.concurrent.atomic.AtomicBoolean(true)

  @SuppressLint("ObjectAnimatorBinding", "CoroutineCreationDuringComposition")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val splashScreen = installSplashScreen() // 必须放在setContent之前
    splashScreen.setKeepOnScreenCondition { mKeepOnAtomicBool.get() } // 使用mKeepOnAtomicBool状态控制欢迎界面
    App.startMicroModuleProcess() // 启动MicroModule
    val enable = this.getBoolean(KEY_ENABLE_AGREEMENT, false) // 获取隐私协议状态

    WindowCompat.setDecorFitsSystemWindows(window, false) // 全屏

    val grant = PromiseOut<Boolean>().also { App.grant = it }

    setContent {
      val scope = rememberCoroutineScope()
      val localPrivacy = LocalCommonUrl.current
      LaunchedEffect(mKeepOnAtomicBool) { // 最多显示1s就需要隐藏欢迎界面
        delay(1000L)
        if (enable) { // 如果已经同意协议了，不需要关闭欢迎界面，直接跳转主页
          grant.resolve(true)
        } else {
          mKeepOnAtomicBool.getAndSet(false)
        }
      }

      DwebBrowserAppTheme {

        SplashMainView()
        if (enable) {
          return@DwebBrowserAppTheme
        }

        SplashPrivacyDialog(
          openHome = {
            App.appContext.saveBoolean(KEY_ENABLE_AGREEMENT, true)
            grant.resolve(true)
          },
          openWebView = { url -> localPrivacy.value = url },
          closeApp = {
            grant.resolve(false)
            finish()
            scope.launch(ioAsyncExceptionHandler) {  // 如果不同意协议就把整个应用停了
              delay(100)
              exitProcess(0)
            }
          }
        )
        CommonWebView()
      }
    }
  }

  override fun onStop() {
    super.onStop()
    finish()
  }
}

@Composable
fun SplashMainView() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    Image(
      imageVector = ImageVector.vectorResource(R.drawable.ic_launcher_foreground),
      contentDescription = "AppIcon",
      modifier = Modifier
        .size(288.dp)
        .align(Alignment.Center)
    )

    Image(
      imageVector = ImageVector.vectorResource(R.drawable.ic_branding),
      contentDescription = "Branding",
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 64.dp)
        .size(width = 192.dp, height = 64.dp)
    )
  }
}