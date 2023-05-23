package info.bagen.dwebbrowser

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.accompanist.web.*
import info.bagen.dwebbrowser.microService.sys.plugin.permission.PermissionManager
import info.bagen.dwebbrowser.ui.browser.HomePage
import info.bagen.dwebbrowser.ui.loading.LoadingView
import info.bagen.dwebbrowser.ui.splash.SplashPrivacyDialog
import info.bagen.dwebbrowser.ui.theme.RustApplicationTheme
import info.bagen.dwebbrowser.util.KEY_ENABLE_AGREEMENT
import info.bagen.dwebbrowser.util.getBoolean
import info.bagen.dwebbrowser.util.permission.PermissionManager.Companion.MY_PERMISSIONS
import info.bagen.dwebbrowser.util.permission.PermissionUtil
import info.bagen.dwebbrowser.util.saveBoolean
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
  @OptIn(DelicateCoroutinesApi::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // 全屏
    val controller = WindowInsetsControllerCompat(window, window.decorView)
    // controller.isAppearanceLightStatusBars = true // false 状态颜色
    controller.hide(WindowInsetsCompat.Type.statusBars())
    controller.hide(WindowInsetsCompat.Type.navigationBars())

    App.startMicroModuleProcess()

    val enable = this.getBoolean(KEY_ENABLE_AGREEMENT, false)
    setContent {
      RustApplicationTheme {
        val webUrl = remember { mutableStateOf("") }
        val showLoading = remember { mutableStateOf(false) }
        // SplashMainView()
        HomePage()
        if (enable) {
          App.grant.resolve(true)
          return@RustApplicationTheme
        }

        SplashPrivacyDialog(
          openHome = {
            // checkAndRequestPermission() // 上架要求不能在这边请求权限，必须等需要用到权限时再请求
            App.appContext.saveBoolean(KEY_ENABLE_AGREEMENT, true)
            App.grant.resolve(true)
          },
          openWebView = { url -> webUrl.value = url },
          closeApp = {
            App.grant.resolve(false)
            finish()
            GlobalScope.launch {  // 如果不统一协议就把整个应用停了
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

  private fun checkAndRequestPermission() {
    if (!PermissionManager.hasPermissions(
        this,
        arrayOf(
          Manifest.permission.READ_PHONE_STATE,
          Manifest.permission.READ_EXTERNAL_STORAGE,
          Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
      )
    ) {
      PermissionManager.requestPermissions(
        this, arrayListOf(
          Manifest.permission.READ_PHONE_STATE,
          Manifest.permission.READ_EXTERNAL_STORAGE,
          Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
      )
    } else {
      App.appContext.saveBoolean(KEY_ENABLE_AGREEMENT, true)
      App.grant.resolve(true)
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == MY_PERMISSIONS) {
      PermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults, this, object : info.bagen.dwebbrowser.util.permission.PermissionManager.PermissionCallback{
        override fun onPermissionGranted(permissions: Array<out String>, grantResults: IntArray) {
          App.appContext.saveBoolean(KEY_ENABLE_AGREEMENT, true)
          App.grant.resolve(true)
        }

        override fun onPermissionDismissed(permission: String) {
        }

        override fun onPositiveButtonClicked(dialog: DialogInterface, which: Int) {
          PermissionUtil.openAppSettings()
        }

        override fun onNegativeButtonClicked(dialog: DialogInterface, which: Int) {
        }
      })
      /*grantResults.forEach {
        if (it != PackageManager.PERMISSION_GRANTED) {
          PermissionManager.requestPermissions(
            this, arrayListOf(
              Manifest.permission.READ_PHONE_STATE,
              Manifest.permission.READ_EXTERNAL_STORAGE,
              Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
          )
          return
        }
      }
      App.appContext.saveBoolean(KEY_ENABLE_AGREEMENT, true)
      App.grant.resolve(true)*/
    }
  }
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

    WebView(state = state, modifier = Modifier.fillMaxSize(),
      client = remember { webViewClient },
      chromeClient = remember { webChromeClient },
      factory = {
        WebView(it).also { webView ->
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
      })
  }
}