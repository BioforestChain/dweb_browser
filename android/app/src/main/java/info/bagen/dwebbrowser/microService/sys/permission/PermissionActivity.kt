package info.bagen.dwebbrowser.microService.sys.permission

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.base.BaseThemeActivity
import kotlinx.coroutines.launch
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
                PermissionController.controller.openAppSettings()
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

  private fun getDenyDialogText(permission: String): String {
    return when (permission) {
      Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR -> {
        App.appContext.getString(R.string.permission_deny_calendar)
      }

      Manifest.permission.CAMERA -> {
        App.appContext.getString(R.string.permission_deny_camera)
      }

      Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS,
      Manifest.permission.GET_ACCOUNTS -> {
        App.appContext.getString(R.string.permission_deny_contacts)
      }

      Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION -> {
        App.appContext.getString(R.string.permission_deny_location)
      }

      Manifest.permission.RECORD_AUDIO -> {
        App.appContext.getString(R.string.permission_deny_record_audio)
      }

      Manifest.permission.READ_PHONE_STATE -> {
        App.appContext.getString(R.string.permission_deny_device)
      }

      Manifest.permission.BODY_SENSORS -> {
        App.appContext.getString(R.string.permission_deny_sensor)
      }

      Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
        App.appContext.getString(R.string.permission_deny_storage)
      }

      Manifest.permission.CALL_PHONE, Manifest.permission.READ_CALL_LOG,
      Manifest.permission.WRITE_CALL_LOG, Manifest.permission.ADD_VOICEMAIL,
      Manifest.permission.USE_SIP, Manifest.permission.PROCESS_OUTGOING_CALLS -> {
        App.appContext.getString(R.string.permission_deny_call)
      }

      Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS,
      Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_WAP_PUSH,
      Manifest.permission.RECEIVE_MMS -> {
        App.appContext.getString(R.string.permission_deny_sms)
      }

      else -> {
        App.appContext.getString(R.string.permission_deny_text)
      }
    }
  }
}