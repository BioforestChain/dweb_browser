package info.bagen.dwebbrowser

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.google.accompanist.web.*
import info.bagen.dwebbrowser.microService.helper.ioAsyncExceptionHandler
import info.bagen.dwebbrowser.ui.browser.setDarkMode
import info.bagen.dwebbrowser.ui.loading.LoadingView
import info.bagen.dwebbrowser.ui.splash.SplashPrivacyDialog
import info.bagen.dwebbrowser.ui.theme.RustApplicationTheme
import info.bagen.dwebbrowser.util.KEY_ENABLE_AGREEMENT
import info.bagen.dwebbrowser.util.getBoolean
import info.bagen.dwebbrowser.util.saveBoolean
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    setContent {
      val scope = rememberCoroutineScope()
      LaunchedEffect(mKeepOnAtomicBool) { // 最多显示1s就需要隐藏欢迎界面
        delay(1000L)
        if (enable) { // 如果已经同意协议了，不需要关闭欢迎界面，直接跳转主页
          App.grant.resolve(true)
        } else {
          mKeepOnAtomicBool.getAndSet(false)
        }
      }

      RustApplicationTheme {
        SideEffect { // 为了全屏
          WindowCompat.setDecorFitsSystemWindows(this@SplashActivity.window, false)
        }
        val webUrl = remember { mutableStateOf("") }
        val showLoading = remember { mutableStateOf(false) }

        SplashMainView()
        if (enable) {
          return@RustApplicationTheme
        }

        SplashPrivacyDialog(
          openHome = {
            App.appContext.saveBoolean(KEY_ENABLE_AGREEMENT, true)
            App.grant.resolve(true)
          },
          openWebView = { url -> webUrl.value = url },
          closeApp = {
            App.grant.resolve(false)
            finish()
            scope.launch(ioAsyncExceptionHandler) {  // 如果不同意协议就把整个应用停了
              delay(100)
              exitProcess(0)
            }
          }
        )
        PrivacyView(url = webUrl, showLoading)
        LoadingView(showLoading)
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PrivacyView(url: MutableState<String>, showLoading: MutableState<Boolean>) {
  BackHandler { url.value = "" }
  val state = WebViewState(WebContent.Url(url.value))
  LaunchedEffect(state) {
    snapshotFlow { state.loadingState }.collect {
      when (it) {
        is LoadingState.Loading -> showLoading.value = true
        else -> showLoading.value = false
      }
    }
  }
  if (url.value.isNotEmpty()) {
    val webViewClient = object : AccompanistWebViewClient() {
      override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest?,
        error: WebResourceError?
      ) {
        super.onReceivedError(view, request, error)
        // android 6.0以下执行
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          return
        }
        // 断网或者网络连接超时
        val errorCode = error?.errorCode
        if (errorCode == ERROR_HOST_LOOKUP || errorCode == ERROR_CONNECT || errorCode == ERROR_TIMEOUT) {
          view.loadUrl("about:blank") // 避免出现默认的错误界面
          //view!!.loadUrl(mErrorUrl) // 加载自定义错误页面
        }
      }

      override fun onReceivedHttpError(
        view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?
      ) {
        // Log.e("SplashActivity", "PrivacyView::onReceivedHttpError $errorResponse")
        super.onReceivedHttpError(view, request, errorResponse)
        // 这个方法在 android 6.0才出现
        val statusCode = errorResponse!!.statusCode
        if (404 == statusCode || 500 == statusCode) {
          view?.loadUrl("about:blank") // 避免出现默认的错误界面
          // view!!.loadUrl(mErrorUrl) // 加载自定义错误页面
        }
      }
    }

    val webChromeClient = object : AccompanistWebChromeClient() {
      override fun onReceivedTitle(view: WebView, title: String?) {
        super.onReceivedTitle(view, title)
        // Log.e("SplashActivity", "SplashActivity::PrivacyView::onReceivedTitle $title")
        // android 6.0 以下通过title获取判断
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
          title?.let {
            if (it.contains("404") || it.contains("500") || it.contains("Error") ||
              it.contains("找不到网页") || it.contains("网页无法打开")
            ) {
              view.loadUrl("about:blank") // 避免出现默认的错误界面
              // view!!.loadUrl(mErrorUrl) // 加载自定义错误页面
            }
          }
        }
      }
    }

    Box(
      modifier = Modifier.clickable(
        indication = null,
        onClick = { },
        interactionSource = remember { MutableInteractionSource() }
      )
    ) {
      val background = MaterialTheme.colorScheme.background
      val isDark = isSystemInDarkTheme()
      WebView(
        state = state,
        modifier = Modifier
          .fillMaxSize()
          .background(background), // TODO 为了避免暗模式突然闪一下白屏
        client = remember { webViewClient },
        chromeClient = remember { webChromeClient },
        factory = {
          WebView(it).also { webView ->
            webView.setDarkMode(isDark, background) // 设置深色主题
            webView.settings.also { settings ->
              settings.javaScriptEnabled = true
              settings.domStorageEnabled = true
              settings.databaseEnabled = true
              settings.safeBrowsingEnabled = true
              settings.loadWithOverviewMode = true
              settings.loadsImagesAutomatically = true
              settings.setSupportMultipleWindows(true)
              settings.allowFileAccess = true
              settings.javaScriptCanOpenWindowsAutomatically = true
              settings.allowContentAccess = true
            }
          }
        }
      )
    }
  }
}