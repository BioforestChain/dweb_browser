package org.dweb_browser.sys.permission

import android.Manifest
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.dweb_browser.core.std.permission.PermissionType
import org.dweb_browser.core.std.permission.debugPermission
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.android.BaseActivity

class PermissionActivity : BaseActivity() {
  companion object {
    var activityPromiseOut: PromiseOut<MutableMap<PermissionType, Boolean>> = PromiseOut()
    val EXTRA_PERMISSION = "permission"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    debugPermission("PermissionActivity", "enter")
    val permissionTypes =
      intent.getSerializableExtra(EXTRA_PERMISSION) as MutableList<PermissionType>
    lifecycleScope.launch {
      val map = mutableMapOf<PermissionType, Boolean>()
      permissionTypes.forEach { type ->
        debugPermission("PermissionActivity", "type=$type")
        val permissions = getActualPermissions(type)
        if (permissions.size == 1) {
          map[type] = requestPermissionLauncher.launch(permissions[0])
        } else if (permissions.size > 1) {
          var grant = false
          requestMultiplePermissionsLauncher.launch(permissions.toTypedArray())
            .forEach { type, granted ->
              if (!granted) {
                grant = false
                return@forEach
              } else {
                grant = true
              }
            }
          map[type] = grant
        }
      }
      activityPromiseOut.resolve(map)
    }
  }
}

private fun getActualPermissions(type: PermissionType): MutableList<String> {
  return when (type) {
    PermissionType.CAMERA -> mutableListOf(Manifest.permission.CAMERA)
    PermissionType.LOCATION -> mutableListOf(
      Manifest.permission.ACCESS_COARSE_LOCATION,
      Manifest.permission.ACCESS_FINE_LOCATION
    )

    PermissionType.STORAGE -> mutableListOf(
      Manifest.permission.WRITE_EXTERNAL_STORAGE,
      Manifest.permission.READ_EXTERNAL_STORAGE
    )

    PermissionType.CALENDAR -> mutableListOf(
      Manifest.permission.READ_CALENDAR,
      Manifest.permission.WRITE_CALENDAR
    )

    PermissionType.CONTACTS -> mutableListOf(
      Manifest.permission.READ_CONTACTS,
      Manifest.permission.WRITE_CONTACTS,
      Manifest.permission.GET_ACCOUNTS
    )

    PermissionType.MICROPHONE -> mutableListOf(
      Manifest.permission.RECORD_AUDIO
    )

    PermissionType.SENSORS -> mutableListOf(
      Manifest.permission.BODY_SENSORS
    )

    PermissionType.SMS -> mutableListOf(
      Manifest.permission.SEND_SMS,
      Manifest.permission.RECEIVE_SMS,
      Manifest.permission.READ_SMS,
      Manifest.permission.RECEIVE_WAP_PUSH,
      Manifest.permission.RECEIVE_MMS,
    )

    PermissionType.PHONE -> mutableListOf(
      Manifest.permission.READ_PHONE_STATE,
    )

    PermissionType.CALL -> mutableListOf(
      Manifest.permission.CALL_PHONE,
      Manifest.permission.READ_CALL_LOG,
      Manifest.permission.WRITE_CALL_LOG,
      Manifest.permission.ADD_VOICEMAIL,
      Manifest.permission.USE_SIP,
      Manifest.permission.PROCESS_OUTGOING_CALLS,
    )

    else -> {
      mutableListOf()
    }
  }
}