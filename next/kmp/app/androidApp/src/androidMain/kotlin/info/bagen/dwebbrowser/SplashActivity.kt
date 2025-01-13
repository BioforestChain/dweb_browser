package info.bagen.dwebbrowser

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.os.Bundle
import android.transition.Fade
import android.view.Window
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.dweb_browser.browser.common.CommonWebView
import org.dweb_browser.browser.common.SplashPrivacyDialog
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.interceptStartApp
import org.dweb_browser.helper.Once1
import org.dweb_browser.helper.addStartActivityOptions
import org.dweb_browser.helper.compose.ENV_SWITCH_KEY
import org.dweb_browser.helper.compose.envSwitch
import org.dweb_browser.helper.getBoolean
import org.dweb_browser.helper.globalMainScope
import org.dweb_browser.helper.platform.theme.DwebBrowserAppTheme
import org.dweb_browser.helper.removeStartActivityOptions
import org.dweb_browser.helper.saveBoolean
import kotlin.system.exitProcess

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
  private var mKeepOnAtomicBool by mutableStateOf(true)
  private val keyEnableAgreement = "enable.agreement" // 判断是否第一次运行程序


  @OptIn(ExperimentalCoroutinesApi::class)
  private val grantInstaller = Once1 { agree: Boolean ->
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

    grant
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @SuppressLint("ObjectAnimatorBinding", "CoroutineCreationDuringComposition")
  override fun onCreate(savedInstanceState: Bundle?) {
//    enableEdgeToEdge() // 全屏
    WindowCompat.setDecorFitsSystemWindows(window, false) // 全屏

    super.onCreate(savedInstanceState)
    with(window) {
      requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
      allowEnterTransitionOverlap = true
      allowReturnTransitionOverlap = true

      exitTransition = Fade()
    }
    addStartActivityOptions(this) {
      ActivityOptions.makeSceneTransitionAnimation(this).toBundle()
    }
    val agree = this.getBoolean(keyEnableAgreement, false) // 获取隐私协议状态
    val grant = grantInstaller(agree)

    setContent {
      var localPrivacy by remember { mutableStateOf("") }

      DwebBrowserAppTheme {
        SplashMainView(
          Modifier,
          startAnimation = !mKeepOnAtomicBool,
        )
        if (agree) {
          return@DwebBrowserAppTheme
        }

        SplashPrivacyDialog(
          openHome = {
            DwebBrowserApp.appContext.saveBoolean(keyEnableAgreement, true)
            // 新安装的用户，默认启用 profile
            envSwitch.enable(ENV_SWITCH_KEY.DWEBVIEW_PROFILE)
            grant.complete(true)
          },
          openWebView = { url -> localPrivacy = url },
          closeApp = {
            grant.complete(false)
          },
        )
        if (localPrivacy.isNotEmpty()) {
          CommonWebView(localPrivacy) { localPrivacy = "" }
        }
      }
    }
  }

  override fun onStop() {
    super.onStop()
    finish()
  }

  override fun onDestroy() {
    super.onDestroy()
    removeStartActivityOptions(this)
  }
}

@Composable
fun dpAni(targetValue: Dp, label: String, onFinished: () -> Unit = {}): Dp {
  return animateDpAsState(targetValue,
    animationSpec = tween(800, easing = EaseInOutQuart),
    label = label,
    finishedListener = { onFinished() }).value
}

@Composable
fun SplashMainView(modifier: Modifier, startAnimation: Boolean) {
  @SuppressLint("UnusedBoxWithConstraintsScope")
  BoxWithConstraints(
    modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.Center,
  ) {
    var aniStart by remember { mutableStateOf(false) }
    val logoHeight = 288.dp//maxHeight * 0.566f
    var logoTop by remember { mutableStateOf(0.dp) }
    val bannerTop = logoHeight / 2 + logoTop

    LaunchedEffect(constraints.maxWidth, constraints.maxHeight) {
      if (startAnimation) {
        delay(10)
        val boxSize = min(maxWidth, maxHeight) * 1.618f
        logoTop = (boxSize * 0.217f) - ((maxHeight - logoHeight) / 2)
        delay(500)
        aniStart = true
      }
    }

    val logoOffsetY = dpAni(logoTop, "logoPaddingTop")
    Image(imageVector = when (BuildConfig.DEBUG) {
      true -> ImageVector.vectorResource(R.drawable.ic_launcher_foreground_debug)
      false -> when (BuildConfig.VERSION_NAME.split("-").last()) {
        "dev" -> ImageVector.vectorResource(R.drawable.ic_launcher_foreground_dev)
        "beta" -> ImageVector.vectorResource(R.drawable.ic_launcher_foreground_beta)
        else -> ImageVector.vectorResource(R.drawable.ic_launcher_foreground_stable)
      }
    },
      contentDescription = "Dweb Browser Logo",
      modifier = Modifier
        .requiredSize(288.dp)
        .offset {
          IntOffset(0, (logoOffsetY.value * density).toInt())
        })
    val bannerOffsetY = dpAni(bannerTop, "bannerPaddingTop")
    Box(
      Modifier.offset {
        IntOffset(0, (bannerOffsetY.value * density).toInt())
      },
      contentAlignment = Alignment.TopCenter,
    ) {
      var brushStartX by remember { mutableFloatStateOf(0.5f) }
      var brushEndX by remember { mutableFloatStateOf(0.5f) }
      var brushColor by remember { mutableStateOf(Color.Transparent) }
      val toColor = MaterialTheme.colorScheme.primary
      if (aniStart) {
        brushStartX = 0f
        brushEndX = 1f
        brushColor = toColor
      }
      val animationSpec =
        remember { tween<Float>(durationMillis = 2000, easing = FastOutSlowInEasing) }
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
          0.5f to toColor,
          endX to color,
          1f to Color.Transparent,
        ),
      )
      Text(
        " Dweb Browser ",
        style = MaterialTheme.typography.headlineLarge.merge(TextStyle(brush = brush)),
      )
    }
  }
}