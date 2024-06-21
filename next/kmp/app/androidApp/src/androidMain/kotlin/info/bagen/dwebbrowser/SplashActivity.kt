package info.bagen.dwebbrowser

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.min
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.browser.common.CommonWebView
import org.dweb_browser.browser.common.SplashPrivacyDialog
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.interceptStartApp
import org.dweb_browser.helper.compose.LocalCommonUrl
import org.dweb_browser.helper.getBoolean
import org.dweb_browser.helper.globalMainScope
import org.dweb_browser.helper.platform.theme.DwebBrowserAppTheme
import org.dweb_browser.helper.saveBoolean
import org.dweb_browser.sys.window.render.NativeBackHandler
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

    val agree = this.getBoolean(keyEnableAgreement, false) // 获取隐私协议状态
    // 启动屏幕的安装 必须放在setContent之前
    val splashScreen = installSplashScreen().also {
      it.setKeepOnScreenCondition { mKeepOnAtomicBool } // 使用mKeepOnAtomicBool状态控制欢迎界面
    }
    // 上启动锁
    val grant = CompletableDeferred<Boolean>()
    grant.invokeOnCompletion {
      if (grant.isCompleted && grant.getCompleted()) {
        mKeepOnAtomicBool = false
      } else {
        // 如果不同意协议就把整个应用停了
        finish()
        globalMainScope.launch {
          delay(100)
          exitProcess(0)
        }
      }
    }
    NativeMicroModule.interceptStartApp(grant);
    if (agree) {
      grant.complete(true)
    } else {
      mKeepOnAtomicBool = false
    }

    /// 启动应用
    DwebBrowserApp.startMicroModuleProcess() // 启动MicroModule

    setContent {
      var localPrivacy by LocalCommonUrl.current

      DwebBrowserAppTheme {
        SplashMainView()
        if (agree) {
          return@DwebBrowserAppTheme
        }

        SplashPrivacyDialog(
          openHome = {
            DwebBrowserApp.appContext.saveBoolean(keyEnableAgreement, true)
            grant.complete(true)
          },
          openWebView = { url -> localPrivacy = url },
          closeApp = {
            grant.complete(false)
          },
        )
        if (localPrivacy.isNotEmpty()) {
          NativeBackHandler { localPrivacy = "" }
          CommonWebView(localPrivacy)
        }
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
  BoxWithConstraints(
    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.TopCenter,
  ) {
    val size = min(maxWidth, maxHeight)
    Column(
      modifier = Modifier.size(size * 1.618f),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(Modifier.weight(0.382f))
      BoxWithConstraints(
        Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center
      ) {
        Image(
          imageVector = ImageVector.vectorResource(R.drawable.ic_launcher_foreground),
          contentDescription = "Dweb Browser Logo",
          modifier = Modifier.size(min(maxWidth, maxHeight))
        )
      }
      Box(Modifier.weight(0.382f), contentAlignment = Alignment.TopCenter) {
        var brushStartX by remember { mutableFloatStateOf(0.5f) }
        var brushEndX by remember { mutableFloatStateOf(0.5f) }
        var brushColor by remember { mutableStateOf(Color.Transparent) }
        val brushToColor = MaterialTheme.colorScheme.primary
        val animationSpec = tween<Float>(durationMillis = 2000, easing = FastOutSlowInEasing)
        val startX by animateFloatAsState(
          brushStartX, label = "startX", animationSpec = animationSpec
        )
        val endX by animateFloatAsState(brushEndX, label = "endX", animationSpec = animationSpec)
        val color by animateColorAsState(
          brushColor,
          label = "color",
          animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
        )
        val brush = Brush.horizontalGradient(
          colorStops = arrayOf(
            0f to Color.Transparent,
            startX to color,
            0.5f to brushToColor,
            endX to color,
            1f to Color.Transparent,
          ),
        )
        Text(" Dweb Browser ", style = MaterialTheme.typography.headlineLarge.merge(
          TextStyle(
            brush = brush,
          )
        ), modifier = Modifier.onGloballyPositioned {
          brushStartX = 0f
          brushEndX = 1f
          brushColor = brushToColor
        })
      }
    }
  }
}