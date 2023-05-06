package info.bagen.dwebbrowser.microService.browser

import android.content.*
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import info.bagen.dwebbrowser.microService.browser.BrowserNMM.Companion.browserController
import info.bagen.dwebbrowser.microService.helper.ioAsyncExceptionHandler
import info.bagen.dwebbrowser.ui.browser.ios.BrowserIntent
import info.bagen.dwebbrowser.ui.browser.ios.BrowserView
import info.bagen.dwebbrowser.ui.camera.QRCodeIntent
import info.bagen.dwebbrowser.ui.camera.QRCodeScanning
import info.bagen.dwebbrowser.ui.camera.QRCodeScanningView
import info.bagen.dwebbrowser.ui.camera.QRCodeViewModel
import info.bagen.dwebbrowser.ui.loading.LoadingView
import info.bagen.dwebbrowser.ui.theme.RustApplicationTheme
import info.bagen.dwebbrowser.util.permission.PermissionManager
import info.bagen.dwebbrowser.util.permission.PermissionUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BrowserActivity : AppCompatActivity() {
  fun getContext() = this
  val qrCodeViewModel: QRCodeViewModel = QRCodeViewModel()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    browserController.activity = this
    setContent {
      WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
        !isSystemInDarkTheme() // 设置状态栏颜色跟着主题走
      RustApplicationTheme {
        Box(modifier = Modifier.fillMaxSize()) {
          BrowserView(viewModel = browserController.browserViewModel)
          QRCodeScanningView(this@BrowserActivity, qrCodeViewModel)
          LoadingView(browserController.showLoading)
        }
      }
    }
    SoftKeyboardStateWatcher(window.decorView, this).also {
      GlobalScope.launch(ioAsyncExceptionHandler) {
        it.isOpened.collect {
          browserController.browserViewModel.handleIntent(BrowserIntent.KeyboardStateChanged(it))
        }
      }
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int, permissions: Array<out String>, grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == PermissionManager.MY_PERMISSIONS) {
      PermissionManager(this@BrowserActivity)
        .onRequestPermissionsResult(requestCode,
          permissions,
          grantResults,
          object :
            PermissionManager.PermissionCallback {
            override fun onPermissionGranted(
              permissions: Array<out String>, grantResults: IntArray
            ) {
              // openScannerActivity()
              qrCodeViewModel.handleIntent(QRCodeIntent.OpenOrHide(true))
            }

            override fun onPermissionDismissed(permission: String) {
            }

            override fun onNegativeButtonClicked(dialog: DialogInterface, which: Int) {
            }

            override fun onPositiveButtonClicked(dialog: DialogInterface, which: Int) {
              PermissionUtil.openAppSettings()
            }
          })
    } else if (requestCode == QRCodeScanning.CAMERA_PERMISSION_REQUEST_CODE) {
      grantResults.forEach {
        if (it != PackageManager.PERMISSION_GRANTED) return
      }
      qrCodeViewModel.handleIntent(QRCodeIntent.OpenOrHide(true))
    }
  }

  override fun onStop() {
    super.onStop()
    if (browserController.showLoading.value) { // 如果已经跳转了，这边直接改为隐藏
      browserController.showLoading.value = false
    }
  }

  override fun onDestroy() {
    // 退出APP关闭服务
    super.onDestroy()
    browserController.activity = null
  }
}

// 监听键盘是否显示
class SoftKeyboardStateWatcher(private val activityRootView: View, private val  context: Context) :
  ViewTreeObserver.OnGlobalLayoutListener {
  private var _opened: MutableStateFlow<Pair<Boolean, Int>> = MutableStateFlow(Pair(false, 0))
  val isOpened: SharedFlow<Pair<Boolean, Int>> = _opened.asStateFlow()

  init {
    activityRootView.viewTreeObserver.addOnGlobalLayoutListener(this)
  }

  override fun onGlobalLayout() {
    //下面这种方式则对软键盘没有设置要求
    val r = Rect()
    //r will be populated with the coordinates of your view that area still visible.
    activityRootView.getWindowVisibleDisplayFrame(r)

    val heightDiff: Int = activityRootView.rootView.height - (r.bottom - r.top)
    if (heightDiff > dpToPx(context, 200f)) { // if more than 100 pixels, its probably a keyboard...
      emit(true, heightDiff)
    } else if (heightDiff < dpToPx(context, 200f)) {
      emit(false, 0)
    }
  }

  private fun emit(open: Boolean, height: Int) {
    GlobalScope.launch { _opened.emit(Pair(open, height)) }
  }

  private fun dpToPx(context: Context, valueInDp: Float): Float {
    val metrics: DisplayMetrics = context.resources.displayMetrics
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics)
  }
}