package org.dweb_browser.browser.common

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import org.dweb_browser.browser.R

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionSingleView(
  permissionState: PermissionState,
  @SuppressLint("ModifierParameter") modifier: Modifier? = null,
  onPermissionDenied: () -> Unit, // 权限申请失败，需要上级界面处理相应逻辑
  permissionNotAvailableContent: @Composable (String) -> Unit = { p ->
    DeniedView(permission = p) { onPermissionDenied(); }
  },
  content: @Composable BoxScope.() -> Unit,
) {

  Box(modifier = modifier ?: Modifier.fillMaxSize()) {
    when (permissionState.status) {
      is PermissionStatus.Granted -> {
        content()
      }

      is PermissionStatus.Denied -> {
        if (!permissionState.status.shouldShowRationale) { // 可以请求权限
          permissionState.launchPermissionRequest()
        } else {
          permissionNotAvailableContent(permissionState.permission)
        }
      }
    }
  }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionMultiView(
  multiplePermissionsState: MultiplePermissionsState,
  @SuppressLint("ModifierParameter") modifier: Modifier? = null,
  onPermissionDenied: () -> Unit, // 权限申请失败，需要上级界面处理相应逻辑
  permissionNotAvailableContent: @Composable (String) -> Unit = { p ->
    DeniedView(permission = p) { onPermissionDenied() }
  },
  content: @Composable () -> Unit,
) {
  Box(modifier = modifier ?: Modifier.fillMaxSize()) {
    when (multiplePermissionsState.allPermissionsGranted) {
      true -> {
        content()
      }

      else -> {
        if (multiplePermissionsState.shouldShowRationale) { // 可以请求权限
          multiplePermissionsState.launchMultiplePermissionRequest()
        } else {
          multiplePermissionsState.permissions.forEach {
            if (!it.status.isGranted) {
              permissionNotAvailableContent(it.permission)
              return@forEach
            }
          }
        }
      }
    }
  }
}

/**
 * 被拒绝后，显示的提示框，用于打开配置界面
 */
@Composable
fun DeniedView(permission: String, onCancel: () -> Unit) {
  val context = LocalContext.current
  var show by remember { mutableStateOf(true) }

  if (show) {
    AlertDialog(
      onDismissRequest = { /*show = false 点击空白区域不隐藏*/ },
      title = { Text(text = "请手动配置权限") },
      text = {
        Text(permissionMaps[permission]?.let { stringResource(id = it) } ?: "请手动配置权限")
      },
      confirmButton = {
        Button(onClick = { onCancel(); show = false; openSettingsPermission(context); }) {
          Text("设置")
        }
      },
      dismissButton = {
        Button(onClick = { onCancel(); show = false; }) {
          Text("取消")
        }
      }
    )
  }
}

fun openSettingsPermission(context: Context) {
  context.startActivity(
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
      data = Uri.fromParts("package", context.packageName, null)
    }
  )
}

val permissionMaps: Map<String, Int> = mutableMapOf<String, Int>().also {
  it[android.Manifest.permission.READ_PHONE_STATE] = R.string.permission_deny_device
  it[android.Manifest.permission.CAMERA] = R.string.permission_deny_camera
  it[android.Manifest.permission.READ_CALENDAR] = R.string.permission_deny_calendar
  it[android.Manifest.permission.WRITE_CALENDAR] = R.string.permission_deny_calendar
  it[android.Manifest.permission.RECORD_AUDIO] = R.string.permission_deny_record_audio
  it[android.Manifest.permission.ACCESS_COARSE_LOCATION] = R.string.permission_deny_location
  it[android.Manifest.permission.BODY_SENSORS] = R.string.permission_deny_sensor
}