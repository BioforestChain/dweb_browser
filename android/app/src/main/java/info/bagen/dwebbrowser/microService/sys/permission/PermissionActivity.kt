package info.bagen.dwebbrowser.microService.sys.permission

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import info.bagen.dwebbrowser.R
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.BaseThemeActivity
import org.dweb_browser.helper.compose.theme.DwebBrowserAppTheme

class PermissionActivity : BaseThemeActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    intent.getStringArrayListExtra("permissions")?.let { permissions ->
      lifecycleScope.launch {
        if (permissions.size == 1) {
          val permission = permissions.first()
          val grant = requestPermissionLauncher.launch(permission)
          if (grant) {
            PermissionController.controller.granted = true
          } else {
            PermissionController.controller.deniedPermission.value = permission
          }
        } else if (permissions.size > 1) {
          val result = requestMultiplePermissionsLauncher.launch(permissions.toTypedArray())
          debugPermission("PermissionActivity", result)
          // 如果返回结果中包含了false，就说明授权失败
          result.forEach { (permission, grant) ->
            if (!grant) {
              PermissionController.controller.deniedPermission.value = permission
              return@forEach
            }
          }
          PermissionController.controller.granted = true
        }
      }
    } ?: run {
      Log.e("PermissionActivity", "no found permission")
      finish()
    }

    setContent {
      DwebBrowserAppTheme {
        PermissionController.controller.deniedPermission.value?.let { permission ->
          AlertDialog(
            onDismissRequest = {
              PermissionController.controller.deniedPermission.value = null
              PermissionController.controller.granted = false
              finish()
            },
            confirmButton = {
              Button(onClick = {
                openAppSettings()
                PermissionController.controller.deniedPermission.value = null
                PermissionController.controller.granted = false
                finish()
              }) {
                Text(text = stringResource(id = R.string.permission_go_settings))
              }
            },
            title = {
              Text(text = stringResource(id = R.string.permission_deny_title))
            },
            text = {
              Text(text = getDenyDialogText(permission = permission))
            }
          )
        }
      }
    }
  }
}