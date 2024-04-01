package org.dweb_browser.sys.contact

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.startAppActivity
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.randomUUID

class ContactPickerActivity : ComponentActivity() {
  companion object {
    private const val EXTRA_TASK_ID_KEY = "taskId"

    private val launchTasks = mutableMapOf<UUID, CompletableDeferred<Uri?>>()
    suspend fun launchAndroidPickerContact(microModule: MicroModule.Runtime): Uri? {
      val taskId = randomUUID()
      return CompletableDeferred<Uri?>().also { task ->
        launchTasks[taskId] = task
        task.invokeOnCompletion {
          launchTasks.remove(taskId)
        }
        microModule.startAppActivity(ContactPickerActivity::class.java) { intent ->
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          intent.putExtra(EXTRA_TASK_ID_KEY, taskId)
        }
      }.await()
    }
  }

  private lateinit var taskId: String
  private val launcherPickerContracts =
    registerForActivityResult(ActivityResultContracts.PickContact()) { uri ->
      launchTasks[taskId]?.complete(uri)
      finish()
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    taskId = (intent.getStringExtra(EXTRA_TASK_ID_KEY) ?: finish()) as String

    lifecycleScope.launch {
      launcherPickerContracts.launch()
    }
  }
}