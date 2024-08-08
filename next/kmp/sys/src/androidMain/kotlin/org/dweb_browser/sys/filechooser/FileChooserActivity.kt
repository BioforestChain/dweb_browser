package org.dweb_browser.sys.filechooser;

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.startAppActivity
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.randomUUID

class FileChooserActivity : ComponentActivity() {
  companion object {
    private val launchTasks = mutableMapOf<UUID, CompletableDeferred<List<String>>>()
    private const val EXTRA_TASK_ID_KEY = "taskId"
    private const val EXTRA_ACCEPT_KEY = "accept"
    private const val EXTRA_MULTI_KEY = "multiple"

    suspend fun launchAndroidFileChooser(
      microModule: MicroModule.Runtime, mimeType: String, multiple: Boolean
    ): List<String> {
      val taskId = randomUUID()
      return CompletableDeferred<List<String>>().also { task ->
        launchTasks[taskId] = task
        task.invokeOnCompletion {
          launchTasks.remove(taskId)
        }
        microModule.startAppActivity(FileChooserActivity::class.java) { intent ->
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          intent.putExtra(EXTRA_TASK_ID_KEY, taskId)
          intent.putExtra(EXTRA_ACCEPT_KEY, mimeType)
          intent.putExtra(EXTRA_MULTI_KEY, multiple)
        }
      }.await()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val taskId = intent.getStringExtra(EXTRA_TASK_ID_KEY) ?: finish()
    val accept = intent.getStringExtra(EXTRA_ACCEPT_KEY)!!
    val multiple = intent.getBooleanExtra(EXTRA_MULTI_KEY, false)

    lifecycleScope.launch {
      when {
        multiple -> {
          registerForActivityResult(ActivityResultContracts.GetMultipleContents()) {
            launchTasks[taskId]?.complete(it.map { uri -> uri.toString() })
            finish()
          }.launch(accept)
        }

        else -> {
          registerForActivityResult(ActivityResultContracts.GetContent()) {
            launchTasks[taskId]?.complete(listOf(it.toString()))
            finish()
          }.launch(accept)
        }
      }
    }
  }
}
