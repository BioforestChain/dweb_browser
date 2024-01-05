package org.dweb_browser.sys.permission

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.startAppActivity
import org.dweb_browser.core.std.permission.AuthorizationStatus
import org.dweb_browser.core.std.permission.debugPermission
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.android.BaseActivity
import org.dweb_browser.helper.getString
import org.dweb_browser.helper.platform.theme.DwebBrowserAppTheme
import org.dweb_browser.helper.randomUUID
import org.dweb_browser.helper.saveString

const val EXTRA_PERMISSION_KEY = "permission"
const val EXTRA_TASK_ID_KEY = "taskId"

private typealias TaskResult = Map<String, AuthorizationStatus>

@Serializable
data class AndroidPermissionTask(
  val permissions: List<String>,
  val title: String,
  val description: String
)

class PermissionActivity : BaseActivity() {
  companion object {
    private val launchTasks = mutableMapOf<UUID, CompletableDeferred<TaskResult>>()
    suspend fun launchAndroidSystemPermissionRequester(
      microModule: MicroModule, androidPermissionTask: AndroidPermissionTask
    ): TaskResult {
      if (androidPermissionTask.permissions.isEmpty()) {
        return emptyMap()
      }
      val taskId = randomUUID()
      return CompletableDeferred<TaskResult>().also { task ->
        launchTasks[taskId] = task
        task.invokeOnCompletion {
          launchTasks.remove(taskId)
        }
        microModule.startAppActivity(PermissionActivity::class.java) { intent ->
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          intent.putExtra(EXTRA_TASK_ID_KEY, taskId)
          intent.putExtra(EXTRA_PERMISSION_KEY, Json.encodeToString(androidPermissionTask))
        }
      }.await()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    debugPermission("PermissionActivity", "onCreate enter")
    val androidPermissionTask = intent.getStringExtra(EXTRA_PERMISSION_KEY)?.let {
      Json.decodeFromString<AndroidPermissionTask>(it)
    } ?: return finish()

    val taskId = intent.getStringExtra(EXTRA_TASK_ID_KEY) ?: return finish()

    val taskResult = (TaskResult::toMutableMap)(mapOf())

    lifecycleScope.launch {
      if (androidPermissionTask.permissions.size == 1) {
        val curPermission = androidPermissionTask.permissions.first()
        taskResult[curPermission] =
          parseAuthorizationStatus(curPermission, requestPermissionLauncher.launch(curPermission))
      } else if (androidPermissionTask.permissions.size > 1) {
        val launchResult =
          requestMultiplePermissionsLauncher.launch(androidPermissionTask.permissions.toTypedArray())
        for ((key, value) in launchResult) {
          taskResult[key] = parseAuthorizationStatus(key, value)
        }
      }
      launchTasks[taskId]?.complete(taskResult)
      finish()
    }

    setContent {
      DwebBrowserAppTheme {
        Box(modifier = Modifier.fillMaxSize()) {
          PermissionTipsView(androidPermissionTask)
        }
      }
    }
  }

  private fun parseAuthorizationStatus(permission: String, grant: Boolean): AuthorizationStatus {
    val status = if (grant) { // 授权成功时执行
      AuthorizationStatus.GRANTED
    } else if (shouldShowRequestPermissionRationale(permission)) { // 第一次请求拒绝时，这个值是true
      AuthorizationStatus.DENIED
    } else if (getString(permission).isNotEmpty()) { // 如果SharePreference存在，表示请求过权限了，那么就是拒绝
      AuthorizationStatus.DENIED
    } else { // 如果上面三个都不满足，说明是取消授权操作，不做saveString
      return AuthorizationStatus.UNKNOWN
    }
    debugPermission(
      "AuthorizationStatus",
      "$permission => $status => $grant => ${shouldShowRequestPermissionRationale(permission)}"
    )
    saveString(permission, Json.encodeToString(status))
    return status
  }
}

@Composable
private fun PermissionTipsView(permissionTips: AndroidPermissionTask) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.primary)
      .shadow(elevation = 2.dp)
  ) {
    ListItem(
      headlineContent = {
        Text(text = permissionTips.title)
      },
      supportingContent = {
        Text(text = permissionTips.description)
      }
    )
  }
}