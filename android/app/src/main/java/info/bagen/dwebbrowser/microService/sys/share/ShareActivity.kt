package info.bagen.dwebbrowser.microService.sys.share

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import info.bagen.dwebbrowser.microService.sys.share.ShareController.Companion.controller
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.dweb_browser.helper.Callback
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.ioAsyncExceptionHandler


class ShareActivity : ComponentActivity() {

  private var isFirstOpen = true
  private val ioAsyncScope = MainScope() + ioAsyncExceptionHandler

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    controller.activity = this
    ShareBroadcastReceiver.resetState()

    controller.shareLauncher =
      registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // 由于 resultCode都是0(Activity.RESULT_CANCEL),data都是null,所以没办法直接通过这个状态判断
        // 这边修改为判断 ShareBroadcastReceiver 是否进行了跳转，如果跳转，默认为分享。
        debugShare(
          "RESULT_SHARE_CODE",
          "data:${result.data} resultCode:${result.resultCode} state:${ShareBroadcastReceiver.shareState}"
        )
        ioAsyncScope.launch {
          getShareSignal.emit(if (ShareBroadcastReceiver.shareState) "OK" else "CANCEL")
        }
      }
  }

  override fun onResume() {
    super.onResume()
    if (isFirstOpen) {
      isFirstOpen = false
    } else {
      finish()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    controller.shareLauncher = null
    ioAsyncScope.cancel()
  }

  private val getShareSignal = Signal<String>()

  fun getShareData(cb: Callback<String>) = getShareSignal.listen(cb)

}