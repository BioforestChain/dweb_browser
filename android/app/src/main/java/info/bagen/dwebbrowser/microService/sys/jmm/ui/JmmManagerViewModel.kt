package info.bagen.dwebbrowser.microService.sys.jmm.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.bagen.dwebbrowser.App
import info.bagen.dwebbrowser.microService.browser.BrowserNMM
import info.bagen.dwebbrowser.microService.sys.jmm.DownLoadObserver
import info.bagen.dwebbrowser.microService.sys.jmm.JmmMetadata
import info.bagen.dwebbrowser.microService.sys.jmm.JmmNMM
import info.bagen.dwebbrowser.service.compareAppVersionHigh
import info.bagen.dwebbrowser.util.DwebBrowserUtil
import info.bagen.dwebbrowser.util.NotificationUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

data class JmmUIState(
    var downloadInfo: MutableState<DownLoadInfo>
)

enum class DownLoadStatus {
    IDLE, DownLoading, DownLoadComplete, PAUSE, INSTALLED, FAIL, CANCEL, NewVersion
}

data class DownLoadInfo(
    var jmmMetadata: JmmMetadata,
    var path: String = "", // 文件下载路径
    var notificationId: Int = 0, // 通知栏的id
    var size: Long = 0L, // 文件大小
    var dSize: Long = 1L, // 已下载大小
    // var progress: Float = 0f, // 进度 0~1
    var downLoadStatus: DownLoadStatus = DownLoadStatus.IDLE, // 标记当前下载状态
)

fun createDownLoadInfoByJmm(jmmMetadata: JmmMetadata): DownLoadInfo {
    return if (JmmNMM.getAndUpdateJmmNmmApps().containsKey(jmmMetadata.id)) {
        // 表示当前mmid已存在，判断版本，如果是同一个版本，显示为打开；如果是更新的版本，显示为 更新
        val curJmmMetadata = JmmNMM.getAndUpdateJmmNmmApps()[jmmMetadata.id]!!.metadata
        if (compareAppVersionHigh(curJmmMetadata.version, jmmMetadata.version)) {
            DownLoadInfo(
                jmmMetadata = jmmMetadata,
                downLoadStatus = DownLoadStatus.NewVersion,
                path = "${App.appContext.cacheDir}/DL_${jmmMetadata.id}_${Calendar.MILLISECOND}.bfsa",
                notificationId = (NotificationUtil.notificationId++),
            )
        } else {
            DownLoadInfo(
                jmmMetadata = jmmMetadata,
                downLoadStatus = DownLoadStatus.INSTALLED
            )
        }
    } else {
        DownLoadInfo(
            jmmMetadata = jmmMetadata,
            downLoadStatus = DownLoadStatus.IDLE,
            path = "${App.appContext.cacheDir}/DL_${jmmMetadata.id}_${Calendar.MILLISECOND}.bfsa",
            notificationId = (NotificationUtil.notificationId++),
        )
    }
}

sealed class JmmIntent {
    object ButtonFunction : JmmIntent()
    object DestroyActivity : JmmIntent()
}

class JmmManagerViewModel(jmmMetadata: JmmMetadata) : ViewModel() {
    val uiState: JmmUIState
    private var downLoadObserver: DownLoadObserver? = null

    init {
        val downLoadInfo = createDownLoadInfoByJmm(jmmMetadata)
        uiState = JmmUIState(mutableStateOf(downLoadInfo))
        if (downLoadInfo.downLoadStatus != DownLoadStatus.INSTALLED) {
            downLoadObserver = DownLoadObserver(jmmMetadata.id)
            initDownLoadStatusListener()
        }
    }

    private fun initDownLoadStatusListener() {
        viewModelScope.launch(Dispatchers.IO) {
            downLoadObserver?.observe {
                if (it.downLoadStatus == DownLoadStatus.IDLE &&
                    uiState.downloadInfo.value.downLoadStatus == DownLoadStatus.NewVersion
                ) {// TODO 为了规避更新被IDLE重置
                    return@observe
                }
                uiState.downloadInfo.value = uiState.downloadInfo.value.copy(
                    downLoadStatus = it.downLoadStatus,
                    dSize = it.downLoadSize,
                    size = it.totalSize
                )
                when (it.downLoadStatus) {
                    DownLoadStatus.DownLoading -> {
                        uiState.downloadInfo.value = uiState.downloadInfo.value.copy(
                            downLoadStatus = it.downLoadStatus,
                            dSize = it.downLoadSize,
                            size = it.totalSize
                        )
                    }
                    else -> {
                        uiState.downloadInfo.value = uiState.downloadInfo.value.copy(
                            downLoadStatus = it.downLoadStatus
                        )
                    }
                }
                if (it.downLoadStatus == DownLoadStatus.INSTALLED) { // 移除监听列表
                    downLoadObserver?.close()
                }
            }
        }
    }

    fun handlerIntent(action: JmmIntent) {
        viewModelScope.launch(Dispatchers.IO) {
            when (action) {
                is JmmIntent.ButtonFunction -> {
                    when (uiState.downloadInfo.value.downLoadStatus) {
                        DownLoadStatus.IDLE, DownLoadStatus.FAIL, DownLoadStatus.CANCEL, DownLoadStatus.NewVersion -> { // 空闲点击是下载，失败点击也是重新下载
                            DwebBrowserUtil.INSTANCE.mBinderService?.invokeDownloadAndSaveZip(
                                uiState.downloadInfo.value
                            )
                        }
                        DownLoadStatus.DownLoadComplete -> { /* TODO 无需响应 */
                        }
                        DownLoadStatus.DownLoading, DownLoadStatus.PAUSE -> {
                            DwebBrowserUtil.INSTANCE.mBinderService?.invokeDownloadStatusChange(
                                uiState.downloadInfo.value.jmmMetadata.id
                            )
                        }
                        DownLoadStatus.INSTALLED -> { // 点击打开app触发的事件
                            BrowserNMM.browserController.openApp(uiState.downloadInfo.value.jmmMetadata.id)
                        }
                    }
                }
                is JmmIntent.DestroyActivity -> {
                    downLoadObserver?.close()
                }
            }
        }
    }
}
