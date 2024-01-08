package org.dweb_browser.helper.android

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.dweb_browser.helper.PromiseOut
import org.dweb_browser.helper.SimpleSignal

abstract class BaseActivity : ComponentActivity() {
  private val queueResultLauncherRegistries = mutableListOf<() -> Unit>()

  /**
   * 对 registerForActivityResult 的易用性封装
   */
  class QueueResultLauncher<I, O>(
    val activity: BaseActivity, val contract: ActivityResultContract<I, O>
  ) {
    private lateinit var launcher: ActivityResultLauncher<I>;

    val tasks = mutableListOf<PromiseOut<O>>()

    init {
      activity.queueResultLauncherRegistries.add {
        launcher = activity.registerForActivityResult(contract) {
          tasks.removeFirst().resolve(it)
        }
      }
    }

    suspend fun launch(input: I): O {
      val task = PromiseOut<O>();
      val preTask = tasks.lastOrNull()
      tasks.add(task)
      /// 如果有上一个任务，那么等待上一个任务完成
      preTask?.waitPromise()
      /// 启动执行器
      launcher.launch(input)
      return task.waitPromise()
    }
  }

  val requestPermissionLauncher =
    QueueResultLauncher(this, ActivityResultContracts.RequestPermission())

  val requestMultiplePermissionsLauncher =
    QueueResultLauncher(this, ActivityResultContracts.RequestMultiplePermissions())


  suspend fun requestPermission(permission: String): Boolean {
    if (checkPermission(permission)) {
      return true
    }
    return requestPermissionLauncher.launch(permission)
  }

  suspend fun checkPermission(permission: String): Boolean {
    return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
  }

  val getContentLauncher = QueueResultLauncher(this, ActivityResultContracts.GetContent())
  val getMultipleContentsLauncher =
    QueueResultLauncher(this, ActivityResultContracts.GetMultipleContents())

  val recordSoundLauncher =
    QueueResultLauncher(this, ExtensionResultContracts.RecordSound())


  val captureVideoLauncher =
    QueueResultLauncher(this, ActivityResultContracts.CaptureVideo())

  val takePictureLauncher =
    QueueResultLauncher(this, ActivityResultContracts.TakePicture())


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    queueResultLauncherRegistries.forEach { it() }
  }

//  fun setContent(content: @Composable () -> Unit) {
//    (this as ComponentActivity).setContent {
//      CompositionLocalProvider(LocalPlatformViewController provides PlatformViewController(this)) {
//        content()
//      }
//    }
//  }

  private val onDestroySignal = SimpleSignal()

  val onDestroyActivity = onDestroySignal.toListener()

  override fun onDestroy() {
    super.onDestroy()
    lifecycleScope.launch {
      onDestroySignal.emit()
    }
  }
}