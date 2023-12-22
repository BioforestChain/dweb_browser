package org.dweb_browser.sys.permission

import android.Manifest
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.dweb_browser.core.std.permission.PermissionType
import org.dweb_browser.core.std.permission.debugPermission
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.android.BaseActivity
import org.dweb_browser.helper.platform.theme.DwebBrowserAppTheme
import org.dweb_browser.sys.SysI18nResource

data class PermissionTips(
  val show: Boolean = true,
  val title: String,
  val message: String,
)

fun rememberPermissionTips(
  show: Boolean = false,
  title: String = "title",
  message: String = "message",
): MutableState<PermissionTips> {
  return mutableStateOf(PermissionTips(show, title, message))
}

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
    val permissionTips = rememberPermissionTips()

    lifecycleScope.launch {
      val map = mutableMapOf<PermissionType, Boolean>()
      for (type in permissionTypes) {
        debugPermission("PermissionActivity", "type=$type")
        val (permissions, permissionTip) = getActualPermissions(type)
        for (permission in permissions) {
          if (checkPermission(permission)) {
            map[type] = true
          } else {
            permissionTips.value = permissionTip.copy(
              show = true, title = permissionTip.title, message = permissionTip.message
            )
            map[type] = requestPermissionLauncher.launch(permission)
          }
        }
//        if (permissions.size == 1) {
//          if (checkPermission(permissions.first())) {
//            map[type] = true
//          } else {
//            permissionTips.value = permissionTip.copy(
//              show = true, title = permissionTip.title, message = permissionTip.message
//            )
//            map[type] = requestPermissionLauncher.launch(permissions[0])
//          }
//        } else if (permissions.size > 1) {
//          var grant = false
//          val permissionRet = requestMultiplePermissionsLauncher.launch(permissions.toTypedArray())
//          for (result in permissionRet) {
//            if (!result.value) {
//              grant = false
//              break
//            } else {
//              grant = true
//            }
//          }
//          map[type] = grant
//        }
      }
      activityPromiseOut.resolve(map)
      finish()
    }

    setContent {
      DwebBrowserAppTheme {
        Box(modifier = Modifier.fillMaxSize()) {
          PermissionTipsView(permissionTips)
        }
      }
    }
  }
}

@Composable
private fun PermissionTipsView(permissionTips: MutableState<PermissionTips>) {
  if (!permissionTips.value.show) return
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.primary)
      .padding(16.dp)
  ) {
    Text(text = permissionTips.value.title, color = MaterialTheme.colorScheme.background)
    Text(
      text = permissionTips.value.message,
      fontSize = 12.sp,
      color = MaterialTheme.colorScheme.background
    )
  }
}

private fun getActualPermissions(
  type: PermissionType
): Pair<MutableList<String>, PermissionTips> {
  val permissionList = mutableListOf<String>()
  val permissionTips: PermissionTips
  when (type) {
    PermissionType.CAMERA -> {
      permissionList.add(Manifest.permission.CAMERA)
      permissionTips = PermissionTips(
        title = SysI18nResource.permission_tip_camera_title.text,
        message = SysI18nResource.permission_tip_camera_message.text
      )
    }

    PermissionType.LOCATION -> {
      permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION)
      permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION)
      permissionTips = PermissionTips(
        title = SysI18nResource.permission_tip_location_title.text,
        message = SysI18nResource.permission_tip_location_message.text
      )
    }

    PermissionType.STORAGE -> {
      permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
      permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
      permissionTips = PermissionTips(title = "null", message = "null")
    }

    PermissionType.CALENDAR -> {
      permissionList.add(Manifest.permission.READ_CALENDAR)
      permissionList.add(Manifest.permission.WRITE_CALENDAR)
      permissionTips = PermissionTips(title = "null", message = "null")
    }

    PermissionType.CONTACTS -> {
      permissionList.add(Manifest.permission.READ_CONTACTS)
      permissionList.add(Manifest.permission.WRITE_CONTACTS)
      permissionList.add(Manifest.permission.GET_ACCOUNTS)
      permissionTips = PermissionTips(title = "null", message = "null")
    }

    PermissionType.MICROPHONE -> {
      permissionList.add(Manifest.permission.RECORD_AUDIO)
      permissionTips = PermissionTips(title = "null", message = "null")
    }

    PermissionType.SENSORS -> {
      permissionList.add(Manifest.permission.BODY_SENSORS)
      permissionTips = PermissionTips(title = "null", message = "null")
    }

    PermissionType.SMS -> {
      permissionList.add(Manifest.permission.SEND_SMS)
      permissionList.add(Manifest.permission.RECEIVE_SMS)
      permissionList.add(Manifest.permission.READ_SMS)
      permissionList.add(Manifest.permission.RECEIVE_WAP_PUSH)
      permissionList.add(Manifest.permission.RECEIVE_MMS)
      permissionTips = PermissionTips(title = "null", message = "null")
    }

    PermissionType.PHONE -> {
      permissionList.add(Manifest.permission.READ_PHONE_STATE)
      permissionTips = PermissionTips(title = "null", message = "null")
    }

    PermissionType.CALL -> {
      permissionList.add(Manifest.permission.CALL_PHONE)
      permissionList.add(Manifest.permission.READ_CALL_LOG)
      permissionList.add(Manifest.permission.WRITE_CALL_LOG)
      permissionList.add(Manifest.permission.ADD_VOICEMAIL)
      permissionList.add(Manifest.permission.USE_SIP)
      permissionList.add(Manifest.permission.PROCESS_OUTGOING_CALLS)
      permissionTips = PermissionTips(title = "null", message = "null")
    }

    else -> {
      permissionTips = PermissionTips(false, "title", "message")
    }
  }
  return Pair(permissionList, permissionTips)
}