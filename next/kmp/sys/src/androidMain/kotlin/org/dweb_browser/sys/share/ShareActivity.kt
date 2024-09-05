package org.dweb_browser.sys.share

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.launch
import org.dweb_browser.core.http.router.ResponseException
import org.dweb_browser.sys.share.ShareController.Companion.controller

class ShareActivity : ComponentActivity() {

  private var isFirstOpen = true
  private var openShareApp = false // 判断是否有打开启动应用，如果没打开默认按照取消操作

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    controller.activity = this

    controller.shareLauncher = registerForActivityResult(
      ActivityResultContracts.StartActivityForResult()
    ) { result ->
      val data = result.data
      val code = data?.getStringExtra("result")
      debugShare("RESULT_SHARE_CODE", "data:$data resultCode:${result.resultCode} code:$code")
      lifecycleScope.launch {
        controller.getShareSignal.emit(
          ResponseException(
            code = HttpStatusCode.OK, message = if (openShareApp) data?.dataString ?: "OK" else ""
          )
        )
      }
    }
  }

  override fun onStop() {
    super.onStop()
    openShareApp = true
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
  }
}