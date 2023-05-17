package info.bagen.dwebbrowser.ui.view

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.R

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionSingleView(
  permission: String = android.Manifest.permission.READ_PHONE_STATE,
  @SuppressLint("ModifierParameter") modifier: Modifier? = null,
  permissionNotAvailableContent: @Composable (String) -> Unit = { p -> DeniedView(permission = p) },
  content: @Composable BoxScope.() -> Unit
) {
  val permissionState = rememberPermissionState(permission)

  Box(modifier = modifier ?: Modifier.fillMaxSize()) {
    when (permissionState.status) {
      PermissionStatus.Granted -> { content() }
      else -> {
        if (permissionState.status.shouldShowRationale) { // 可以请求权限
          permissionState.launchPermissionRequest()
        } else {
          permissionNotAvailableContent(permission)
        }
      }
    }
  }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionMultiView(
  permissions: List<String> = arrayListOf(android.Manifest.permission.READ_PHONE_STATE),
  @SuppressLint("ModifierParameter") modifier: Modifier? = null,
  permissionNotAvailableContent: @Composable (String) -> Unit = { p -> DeniedView(permission = p) },
  content: @Composable () -> Unit
) {
  val permissionState = rememberMultiplePermissionsState(permissions = permissions)
  Box(modifier = modifier ?: Modifier.fillMaxSize()) {
    when (permissionState.allPermissionsGranted) {
      true -> { content() }
      else -> {
        if (permissionState.shouldShowRationale) { // 可以请求权限
          permissionState.launchMultiplePermissionRequest()
        } else {
          permissionState.permissions.forEach {
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
private fun DeniedView(permission: String) {
  val context = LocalContext.current
  AlertDialog(
    onDismissRequest = { /* Don't */ },
    title = { Text(text = "请手动配置权限") },
    text = { Text(permissionMaps[permission] ?: "请手动配置权限") },
    confirmButton = {
      Button(onClick = { openSettingsPermission(context) }) {
        Text("设置")
      }
    }
  )
}

fun openSettingsPermission(context: Context) {
  context.startActivity(
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
      data = Uri.fromParts("package", context.packageName, null)
    }
  )
}

val permissionMaps: Map<String, String> = mutableMapOf<String, String>().also {
  val context = App.appContext
  it[android.Manifest.permission.READ_PHONE_STATE]       = context.getString(R.string.permission_deny_device)
  it[android.Manifest.permission.CAMERA]                 = context.getString(R.string.permission_deny_camera)
  it[android.Manifest.permission.READ_CALENDAR]          = context.getString(R.string.permission_deny_calendar)
  it[android.Manifest.permission.WRITE_CALENDAR]         = context.getString(R.string.permission_deny_calendar)
  it[android.Manifest.permission.RECORD_AUDIO]           = context.getString(R.string.permission_deny_record_audio)
  it[android.Manifest.permission.ACCESS_COARSE_LOCATION] = context.getString(R.string.permission_deny_location)
  it[android.Manifest.permission.BODY_SENSORS]           = context.getString(R.string.permission_deny_sensor)
}