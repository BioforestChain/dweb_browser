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
import org.dweb_browser.helper.platform.theme.DwebBrowserAppTheme
import org.dweb_browser.helper.randomUUID

const val EXTRA_PERMISSION_KEY = "permission"
const val EXTRA_TASK_ID_KEY = "taskId"

private typealias TaskResult = Map<String, AuthorizationStatus>

@Serializable
data class AndroidPermissionTask(val key: String, val title: String, val description: String)

class PermissionActivity : BaseActivity() {
  companion object {
    private val launchTasks = mutableMapOf<UUID, CompletableDeferred<TaskResult>>()
    suspend fun launchAndroidSystemPermissionRequester(
      microModule: MicroModule,
      vararg tasks: AndroidPermissionTask
    ): TaskResult {
      if (tasks.isEmpty()) {
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
          intent.putExtra(EXTRA_PERMISSION_KEY, Json.encodeToString(tasks))
        }
      }.await()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    debugPermission("PermissionActivity", "onCreate enter")
    val taskList = (intent.getStringExtra(EXTRA_PERMISSION_KEY)
      ?: return finish()).let { Json.decodeFromString<List<AndroidPermissionTask>>(it) }
    val taskId = intent.getStringExtra(EXTRA_TASK_ID_KEY) ?: return finish()

    val taskResult = (TaskResult::toMutableMap)(mapOf())

    lifecycleScope.launch {
      if (taskList.size == 1) {
        val task = taskList.first()
        taskResult[task.key] = requestPermissionLauncher.launch(task.key)
          .let { if (it) AuthorizationStatus.GRANTED else AuthorizationStatus.UNKNOWN }
      } else {
        val launchResult =
          requestMultiplePermissionsLauncher.launch(taskList.map { it.key }.toTypedArray())
        for ((key, value) in launchResult) {
          taskResult[key] = if (value) AuthorizationStatus.GRANTED else AuthorizationStatus.UNKNOWN
        }
      }
      launchTasks[taskId]?.complete(taskResult)

      finish()
    }

    setContent {
      DwebBrowserAppTheme {
        Box(modifier = Modifier.fillMaxSize()) {
          PermissionTipsView(taskList)
        }
      }
    }
  }
}

@Composable
private fun PermissionTipsView(permissionTips: List<AndroidPermissionTask>) {

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.primary)
      .shadow(elevation = 2.dp)
  ) {
    for (tip in permissionTips) {
      ListItem(
        headlineContent = {
          Text(text = tip.title)
        },
        supportingContent = {
          Text(text = tip.description)
        }
      )
    }
  }
}

//private fun getActualPermissions(
//  type: SystemPermissionName
//): Pair<MutableList<String>, PermissionTip> {
//  val permissionList = mutableListOf<String>()
//  val permissionTip: PermissionTip
//  when (type) {
//    SystemPermissionName.CAMERA -> {
//      permissionList.add(Manifest.permission.CAMERA)
//      permissionTip = PermissionTip(
//        title = SysI18nResource.permission_tip_camera_title.text,
//        description = SysI18nResource.permission_tip_camera_message.text
//      )
//    }
//
//    SystemPermissionName.LOCATION -> {
//      permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION)
//      permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION)
//      permissionTip = PermissionTip(
//        title = SysI18nResource.permission_tip_location_title.text,
//        description = SysI18nResource.permission_tip_location_message.text
//      )
//    }
//
//    SystemPermissionName.STORAGE -> {
//      permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
//      permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
//      permissionTip = PermissionTip(title = "null", description = "null")
//    }
//
//    SystemPermissionName.CALENDAR -> {
//      permissionList.add(Manifest.permission.READ_CALENDAR)
//      permissionList.add(Manifest.permission.WRITE_CALENDAR)
//      permissionTip = PermissionTip(title = "null", description = "null")
//    }
//
//    SystemPermissionName.CONTACTS -> {
//      permissionList.add(Manifest.permission.READ_CONTACTS)
//      permissionList.add(Manifest.permission.WRITE_CONTACTS)
//      permissionList.add(Manifest.permission.GET_ACCOUNTS)
//      permissionTip = PermissionTip(title = "null", description = "null")
//    }
//
//    SystemPermissionName.MICROPHONE -> {
//      permissionList.add(Manifest.permission.RECORD_AUDIO)
//      permissionTip = PermissionTip(title = "null", description = "null")
//    }
//
//    SystemPermissionName.SENSORS -> {
//      permissionList.add(Manifest.permission.BODY_SENSORS)
//      permissionTip = PermissionTip(title = "null", description = "null")
//    }
//
//    SystemPermissionName.SMS -> {
//      permissionList.add(Manifest.permission.SEND_SMS)
//      permissionList.add(Manifest.permission.RECEIVE_SMS)
//      permissionList.add(Manifest.permission.READ_SMS)
//      permissionList.add(Manifest.permission.RECEIVE_WAP_PUSH)
//      permissionList.add(Manifest.permission.RECEIVE_MMS)
//      permissionTip = PermissionTip(title = "null", description = "null")
//    }
//
//    SystemPermissionName.PHONE -> {
//      permissionList.add(Manifest.permission.READ_PHONE_STATE)
//      permissionTip = PermissionTip(title = "null", description = "null")
//    }
//
//    SystemPermissionName.CALL -> {
//      permissionList.add(Manifest.permission.CALL_PHONE)
//      permissionList.add(Manifest.permission.READ_CALL_LOG)
//      permissionList.add(Manifest.permission.WRITE_CALL_LOG)
//      permissionList.add(Manifest.permission.ADD_VOICEMAIL)
//      permissionList.add(Manifest.permission.USE_SIP)
//      permissionList.add(Manifest.permission.PROCESS_OUTGOING_CALLS)
//      permissionTip = PermissionTip(title = "null", description = "null")
//    }
//
//    else -> {
//      permissionTip = PermissionTip(false, "title", "message")
//    }
//  }
//  return Pair(permissionList, permissionTip)
//}