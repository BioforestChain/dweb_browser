package org.dweb_browser.sys.camera

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.startAppActivity
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.randomUUID
import java.io.File

class CameraActivity : ComponentActivity() {
  companion object {
    private const val EXTRA_TASK_ID_KEY = "taskId"
    private const val EXTRA_TYPE_KEY = "type"
    private const val LaunchTakePicture = 0
    private const val LaunchCaptureVideo = 1

    private val launchTasks = mutableMapOf<UUID, CompletableDeferred<String>>()
    suspend fun launchAndroidTakePicture(microModule: MicroModule): String {
      val taskId = randomUUID()
      return CompletableDeferred<String>().also { task ->
        launchTasks[taskId] = task
        task.invokeOnCompletion {
          launchTasks.remove(taskId)
        }
        microModule.startAppActivity(CameraActivity::class.java) { intent ->
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          intent.putExtra(EXTRA_TASK_ID_KEY, taskId)
          intent.putExtra(EXTRA_TYPE_KEY, LaunchTakePicture)
        }
      }.await()
    }

    suspend fun launchAndroidCaptureVideo(microModule: MicroModule): String {
      val taskId = randomUUID()
      return CompletableDeferred<String>().also { task ->
        launchTasks[taskId] = task
        task.invokeOnCompletion {
          launchTasks.remove(taskId)
        }
        microModule.startAppActivity(CameraActivity::class.java) { intent ->
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          intent.putExtra(EXTRA_TASK_ID_KEY, taskId)
          intent.putExtra(EXTRA_TYPE_KEY, LaunchCaptureVideo)
        }
      }.await()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val taskId = intent.getStringExtra(EXTRA_TASK_ID_KEY) ?: finish()
    val launchType = intent.getIntExtra(EXTRA_TYPE_KEY, 0)

    lifecycleScope.launch {
      when (launchType) {
        LaunchTakePicture -> {
          val tmpFile = File.createTempFile("temp_capture", ".jpg", cacheDir);
          val tmpUri = FileProvider.getUriForFile(
            this@CameraActivity, "$packageName.file.opener.provider", tmpFile
          )
          registerForActivityResult(ActivityResultContracts.TakePicture()) {
            launchTasks[taskId]?.complete(if (it) tmpUri.toString() else "")
            finish()
          }.launch(tmpUri)
        }

        LaunchCaptureVideo -> {
          val tmpFile = File.createTempFile("temp_capture", ".mp4", cacheDir);
          val tmpUri = FileProvider.getUriForFile(
            this@CameraActivity, "$packageName.file.opener.provider", tmpFile
          )
          registerForActivityResult(ActivityResultContracts.CaptureVideo()) {
            launchTasks[taskId]?.complete(if (it) tmpUri.toString() else "")
            finish()
          }.launch(tmpUri)
        }

        else -> {
          launchTasks[taskId]?.complete("")
          finish()
        }
      }
    }
  }

}