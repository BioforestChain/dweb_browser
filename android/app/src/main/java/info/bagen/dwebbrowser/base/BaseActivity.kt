package info.bagen.dwebbrowser.base

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import info.bagen.dwebbrowser.microService.helper.PromiseOut
import info.bagen.dwebbrowser.microService.helper.SimpleCallback
import info.bagen.dwebbrowser.microService.helper.SimpleSignal
import info.bagen.dwebbrowser.microService.helper.ioAsyncExceptionHandler
import info.bagen.dwebbrowser.ui.theme.RustApplicationTheme
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

abstract class BaseActivity : ComponentActivity() {
    private val queueResultLauncherRegistries = mutableListOf<() -> Unit>()

    /**
     * 对 registerForActivityResult 的易用性封装
     */
    final class QueueResultLauncher<I, O>(
        val activity: BaseActivity,
        val contract: ActivityResultContract<I, O>
    ) {
        private lateinit var launcher: ActivityResultLauncher<I>;

        val tasks = mutableListOf<PromiseOut<O>>()

        init {
            activity.lifecycleScope.launch {
                activity.queueResultLauncherRegistries.add {
                    launcher = activity.registerForActivityResult(contract) {
                        tasks.removeFirst().resolve(it);
                    }
                }
            }
        }

        suspend fun launch(input: I): O {
            val task = PromiseOut<O>();
            var preTask = tasks.lastOrNull()
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

    suspend fun requestSelfPermission(permission: String): Boolean {
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        if (requestPermissionLauncher.launch(permission)) {
            return true
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        for (reg in queueResultLauncherRegistries) {
            reg()
        }
        initData()
        setContent {
            RustApplicationTheme {
                WindowCompat.getInsetsController(
                    window,
                    window.decorView
                ).isAppearanceLightStatusBars =
                    !isSystemInDarkTheme() // 设置状态栏颜色跟着主题走
                InitViews()
            }
        }
    }

    open fun initData() {} // 初始化数据，或者注册监听

    @Composable
    open fun InitViews() {
    }// 填充Compose布局


    private val onDestroySignal = SimpleSignal()

    fun onDestroyActivity(cb: SimpleCallback) = onDestroySignal.listen(cb)

    override fun onDestroy() {
        super.onDestroy()
        GlobalScope.launch(ioAsyncExceptionHandler) {
            onDestroySignal.emit()
        }
    }
}