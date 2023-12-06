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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.browser.common.CommonWebView
import org.dweb_browser.browser.common.SplashPrivacyDialog
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.interceptStartApp
import org.dweb_browser.helper.compose.LocalCommonUrl
import org.dweb_browser.helper.getBoolean
import org.dweb_browser.helper.mainAsyncExceptionHandler
import org.dweb_browser.helper.platform.theme.DwebBrowserAppTheme
import org.dweb_browser.helper.saveBoolean
import kotlin.system.exitProcess

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
  private var mKeepOnAtomicBool by atomic(true)
  private val keyEnableAgreement = "enable.agreement" // 判断是否第一次运行程序

  @OptIn(ExperimentalCoroutinesApi::class)
  @SuppressLint("ObjectAnimatorBinding", "CoroutineCreationDuringComposition")
  override fun onCreate(savedInstanceState: Bundle?) {
//    enableEdgeToEdge() // 全屏
    WindowCompat.setDecorFitsSystemWindows(window, false) // 全屏

    super.onCreate(savedInstanceState)

    val enable = this.getBoolean(keyEnableAgreement, false) // 获取隐私协议状态
    // 启动屏幕的安装 必须放在setContent之前
    val splashScreen = installSplashScreen().also {
      it.setKeepOnScreenCondition { mKeepOnAtomicBool } // 使用mKeepOnAtomicBool状态控制欢迎界面
    }
    // 上启动锁
    val grant = CompletableDeferred<Boolean>().also { grant ->
      NativeMicroModule.interceptStartApp(grant);
      grant.invokeOnCompletion {
        if (grant.isCompleted && grant.getCompleted()) {
          mKeepOnAtomicBool = false
        } else {
          // 如果不同意协议就把整个应用停了
          finish()
          CoroutineScope(mainAsyncExceptionHandler).launch {
            delay(100)
            exitProcess(0)
          }
        }
      }
    }
    if (enable) {
      grant.complete(true)
    } else {
      mKeepOnAtomicBool = false
    }

    /// 启动应用
    DwebBrowserApp.startMicroModuleProcess() // 启动MicroModule


    setContent {
      val localPrivacy = LocalCommonUrl.current

      DwebBrowserAppTheme {
        SplashMainView()
        if (enable) {
          return@DwebBrowserAppTheme
        }

        SplashPrivacyDialog(
          openHome = {
            DwebBrowserApp.appContext.saveBoolean(keyEnableAgreement, true)
            grant.complete(true)
          },
          openWebView = { url -> localPrivacy.value = url },
          closeApp = {
            grant.complete(false)
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