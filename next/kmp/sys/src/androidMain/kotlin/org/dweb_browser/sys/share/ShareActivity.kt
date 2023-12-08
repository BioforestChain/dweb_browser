package org.dweb_browser.sys.share

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.sys.share.ShareController.Companion.controller

class ShareActivity : ComponentActivity() {

  private var isFirstOpen = true
  private val ioAsyncScope = MainScope() + ioAsyncExceptionHandler

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    controller.activity = this

    controller.shareLauncher =
      registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data: Intent? = result.data
        val code = data?.getStringExtra("result")
        debugShare(
          "RESULT_SHARE_CODE",
          "data:${result.data} resultCode:${result.resultCode} code:$code"
        )
        ioAsyncScope.launch {
          controller.getShareSignal.emit(data?.dataString ?: "OK")
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
}