package info.bagen.libappmgr.schedule

import android.util.Log
import info.bagen.libappmgr.ui.main.MainViewModel
import info.bagen.libappmgr.utils.FilesUtil
import kotlinx.coroutines.*

class CoroutineUpdateTask : UpdateTask {

    private var scope: CoroutineScope? = null

    override fun scheduleUpdate(interval: Long) {
        cancle()
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            var count = 0 // 用于判断软件运行分钟数，主要为了和应用信息中的maxAge做校验，确认是否需要更新
            while (isActive) {
                Log.d("CoroutineUpdateTask", "scheduleUpdate->$count")
                try {
                    // Todo 开始执行轮询操作
                    FilesUtil.getScheduleAppList().forEach { appInfo ->
                        appInfo.autoUpdate?.let { autoUpdateInfo ->
                            if (count % autoUpdateInfo.maxAge == 0) {
                                val mainViewModel = MainViewModel()
                                mainViewModel.getAppVersionAndSave(appInfo)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                count++
                delay(interval)
            }
        }
        this.scope = scope
    }

    override fun cancle() {
        scope?.cancel()
        scope = null
    }
}
