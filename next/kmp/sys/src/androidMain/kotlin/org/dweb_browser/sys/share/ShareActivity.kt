package org.dweb_browser.sys.share

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.dweb_browser.sys.share.ShareController.Companion.controller

class ShareActivity : ComponentActivity() {

  private var isFirstOpen = true

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
        lifecycleScope.launch {
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
  }
}