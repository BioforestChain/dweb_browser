package org.dweb_browser.sys.camera

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.startAppActivity
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.compose.clickableWithNoEffect
import org.dweb_browser.helper.platform.theme.DwebBrowserAppTheme
import org.dweb_browser.helper.randomUUID
import java.io.File

class CameraActivity : ComponentActivity() {
  companion object {
    private const val EXTRA_TASK_ID_KEY = "taskId"
    private const val EXTRA_TYPE_KEY = "type"
    private const val LaunchTakePicture = 0
    private const val LaunchCaptureVideo = 1
    private const val LaunchGetPhoto = 2

    private val launchTasks = mutableMapOf<UUID, CompletableDeferred<Uri?>>()
    suspend fun launchAndroidTakePicture(microModule: MicroModule): Uri? {
      val taskId = randomUUID()
      return CompletableDeferred<Uri?>().also { task ->
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

    suspend fun launchAndroidCaptureVideo(microModule: MicroModule): Uri? {
      val taskId = randomUUID()
      return CompletableDeferred<Uri?>().also { task ->
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

    suspend fun launchAndroidGetPhoto(microModule: MicroModule): Uri? {
      val taskId = randomUUID()
      return CompletableDeferred<Uri?>().also { task ->
        launchTasks[taskId] = task
        task.invokeOnCompletion {
          launchTasks.remove(taskId)
        }
        microModule.startAppActivity(CameraActivity::class.java) { intent ->
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          intent.putExtra(EXTRA_TASK_ID_KEY, taskId)
          intent.putExtra(EXTRA_TYPE_KEY, LaunchGetPhoto)
        }
      }.await()
    }
  }

  private lateinit var taskId: String
  private var tempUri: Uri? = null
  private val launcherTakePicture =
    registerForActivityResult(ActivityResultContracts.TakePicture()) {
      launchTasks[taskId]?.complete(if (it) tempUri else null)
      finish()
    }
  private val launcherCaptureVideo =
    registerForActivityResult(ActivityResultContracts.CaptureVideo()) {
      launchTasks[taskId]?.complete(if (it) tempUri else null)
      finish()
    }
  private val launcherGetContent =
    registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
      launchTasks[taskId]?.complete(uri)
      finish()
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      window.decorView.windowInsetsController?.apply {
        hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
      }
    }

    taskId = (intent.getStringExtra(EXTRA_TASK_ID_KEY) ?: finish()) as String
    val launchType = intent.getIntExtra(EXTRA_TYPE_KEY, -1)
    val showChoose = mutableStateOf(false)

    lifecycleScope.launch {
      when (launchType) {
        LaunchTakePicture -> {
          val tmpFile = File.createTempFile("temp_capture", ".jpg", cacheDir);
          tempUri = FileProvider.getUriForFile(
            this@CameraActivity, "$packageName.file.opener.provider", tmpFile
          )
          launcherTakePicture.launch(tempUri)
        }

        LaunchCaptureVideo -> {
          val tmpFile = File.createTempFile("temp_capture", ".mp4", cacheDir);
          tempUri = FileProvider.getUriForFile(
            this@CameraActivity, "$packageName.file.opener.provider", tmpFile
          )
          launcherCaptureVideo.launch(tempUri)
        }

        LaunchGetPhoto -> {
          showChoose.value = true
          // launcherGetContent.launch("image/*")
        }

        else -> {
          launchTasks[taskId]?.complete(null)
          finish()
        }
      }
    }

    setContent {
      DwebBrowserAppTheme {
        if (showChoose.value) {
          Box(
            modifier = Modifier.fillMaxSize()
              .background(MaterialTheme.colorScheme.primary.copy(0.3f))
              .clickableWithNoEffect { finish() }) {
            Card(
              modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(96.dp)
                .clickableWithNoEffect { /* 不响应 */ },
              shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp
              )
            ) {
              Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
              ) {
                ChooseItem(Icons.Default.Photo, CameraI18nResource.choose_photo.text) {

                }
                ChooseItem(Icons.Default.PhotoCamera, CameraI18nResource.choose_camera.text) {

                }
              }
            }
          }
        }
      }
    }
  }

  @Composable
  fun ChooseItem(vector: ImageVector, title: String, onClick: () -> Unit) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = vector,
        contentDescription = title,
        modifier = Modifier.size(48.dp).clickable { onClick() })
      Text(text = title)
    }
  }
}