package org.dweb_browser.sys.microphone

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.startAppActivity
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.android.ExtensionResultContracts
import org.dweb_browser.helper.randomUUID
import java.io.File

class MicroPhoneActivity : ComponentActivity() {
  companion object {
    private const val EXTRA_TASK_ID_KEY = "taskId"
    private val launchTasks = mutableMapOf<UUID, CompletableDeferred<String>>()
    suspend fun launchAndroidRecordSound(microModule: MicroModule): String {
      val taskId = randomUUID()
      return CompletableDeferred<String>().also { task ->
        launchTasks[taskId] = task
        task.invokeOnCompletion {
          launchTasks.remove(taskId)
        }
        microModule.startAppActivity(MicroPhoneActivity::class.java) { intent ->
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          intent.putExtra(EXTRA_TASK_ID_KEY, taskId)
        }
      }.await()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val taskId = intent.getStringExtra(EXTRA_TASK_ID_KEY) ?: finish()
    lifecycleScope.launch {
      val tmpFile = File.createTempFile("temp_capture", ".ogg", cacheDir);
      val tmpUri = FileProvider.getUriForFile(
        this@MicroPhoneActivity, "$packageName.file.opener.provider", tmpFile
      )
      registerForActivityResult(ExtensionResultContracts.RecordSound()) {
        launchTasks[taskId]?.complete(if (it) tmpUri.toString() else "")
        finish()
      }.launch(tmpUri)
    }
  }
}