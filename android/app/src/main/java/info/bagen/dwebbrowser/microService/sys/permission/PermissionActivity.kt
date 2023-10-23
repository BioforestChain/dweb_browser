package info.bagen.dwebbrowser.microService.sys.permission

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import info.bagen.dwebbrowser.base.BaseThemeActivity
import kotlinx.coroutines.launch

class PermissionActivity : BaseThemeActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    intent.getStringArrayListExtra("permissions")?.let { permissions ->
      lifecycleScope.launch {
        if (permissions.size == 1) {
          PermissionController.controller.granted = requestPermissionLauncher.launch(permissions[0])
        } else if (permissions.size > 1) {
          val result = requestMultiplePermissionsLauncher.launch(permissions.toTypedArray())
          debugPermission("PermissionActivity", result)
          // 如果返回结果中包含了false，就说明授权失败
          PermissionController.controller.granted = !result.containsValue(false)
        }
        finish()
      }
    } ?: run {
      Log.e("PermissionActivity", "no found permission")
      finish()
    }
  }
}