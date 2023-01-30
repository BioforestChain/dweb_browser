package info.bagen.libappmgr.ui.download

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.bagen.libappmgr.entity.AppInfo
import info.bagen.libappmgr.network.base.IApiResult
import info.bagen.libappmgr.network.base.fold
import info.bagen.libappmgr.ui.view.DialogInfo
import info.bagen.libappmgr.ui.view.DialogType
import info.bagen.libappmgr.utils.FilesUtil
import info.bagen.libappmgr.utils.ZipUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

data class DownLoadUIState(
    val downLoadState: MutableState<DownLoadState> = mutableStateOf(DownLoadState.IDLE),
    val downLoadProgress: MutableState<Float> = mutableStateOf(0f),
    var dialogInfo: DialogInfo = DialogInfo(),
    var downloadAppInfo: AppInfo? = null,
)

/**
 * IDLE 空闲状态，准备下载
 * LOADING 正在下载状态
 * PAUSE 用于停止下载状态
 * STOP 下载过程中退出时，存储当前状态，并保存为Stop
 * COMPLETED 表示下载完成状态
 * FAILURE 表示下载失败状态
 * INSTALL 表示解压安装
 * CLOSE 表示安装完成了
 */
enum class DownLoadState { IDLE, LOADING, PAUSE, STOP, COMPLETED, FAILURE, INSTALL, CLOSE }

data class DownLoadProgress(
    var current: Long = 0L,
    var total: Long = 0L,
    var progress: Float = 0f,
    var downloadFile: String = "",
    var downloadUrl: String = ""
)

private fun DownLoadProgress.update(
    current: Long? = null, total: Long? = null, progress: Float? = null,
    downloadFile: String? = null, downloadUrl: String? = null
) {
    this.current = current ?: this.current
    this.total = total ?: this.total
    this.progress = progress ?: this.progress
    this.downloadFile = downloadFile ?: this.downloadFile
    this.downloadUrl = downloadUrl ?: this.downloadUrl
}

private fun DownLoadProgress.copy(downLoadProgress: DownLoadProgress) {
    this.current = downLoadProgress.current
    this.total = downLoadProgress.total
    this.progress = downLoadProgress.progress
    this.downloadFile = downLoadProgress.downloadFile
    this.downloadUrl = downLoadProgress.downloadUrl
}

/**
 * MIV中的Intent部分
 */
sealed class DownLoadIntent {
    class LoadDownLoadStateAndDownLoad(val path: String) : DownLoadIntent()
    object DownLoadAndSave : DownLoadIntent()
    object BreakpointDownLoadAndSave : DownLoadIntent()
    object ShowDownLoadState : DownLoadIntent()
    object DownLoadPauseStateChanged : DownLoadIntent()
    object DownLoadSaveState : DownLoadIntent()
    object DecompressFile : DownLoadIntent()
}

class DownLoadViewModel(
    private val repository: DownLoadRepository = DownLoadRepository()
) : ViewModel() {
    // val channel = Channel<DownLoadIntent>(2) // 表示最多两个buffer，超过后挂起
    val uiState = mutableStateOf(DownLoadUIState())
    val mDownLoadProgress = DownLoadProgress()
    private var mDownLoadState = DownLoadState.IDLE

    init {/*
    // 这边是初始化的时候执行的内容
    viewModelScope.launch {
      channel.consumeAsFlow().collect { TODO("这功能是持续监听channel信道的内容，有收到就做处理") }
    }*/
        /*viewModelScope.launch {
          loadDownloadState() // 加载存储的下载进度
          when (mDownLoadState) {
            DownLoadState.IDLE -> handleIntent(DownLoadIntent.DownLoadAndSave)
            DownLoadState.STOP -> handleIntent(DownLoadIntent.BreakpointDownLoadAndSave)
            DownLoadState.PAUSE -> handleIntent(DownLoadIntent.ShowDownLoadState)
            else -> {}
          }
        }*/
    }

    fun handleIntent(action: DownLoadIntent) {
        viewModelScope.launch {
            /* channel.send(action) // 进行发送操作，可以根据传参进行发送 */
            when (action) {
                is DownLoadIntent.LoadDownLoadStateAndDownLoad -> {
                    loadDownloadState(action.path) // 加载存储的下载进度
                    when (mDownLoadState) {
                        DownLoadState.IDLE -> handleIntent(DownLoadIntent.DownLoadAndSave)
                        DownLoadState.STOP -> handleIntent(DownLoadIntent.BreakpointDownLoadAndSave)
                        DownLoadState.PAUSE -> handleIntent(DownLoadIntent.ShowDownLoadState)
                        else -> {}
                    }
                }
                is DownLoadIntent.DownLoadAndSave -> {
                    uiState.value.downLoadState.value = DownLoadState.LOADING
                    downLoadAndSave(mDownLoadProgress.downloadUrl, mDownLoadProgress.downloadFile)
                }
                is DownLoadIntent.BreakpointDownLoadAndSave -> {
                    uiState.value.downLoadState.value = DownLoadState.LOADING
                    breakpointDownLoadAndSave(
                        mDownLoadProgress.downloadUrl,
                        mDownLoadProgress.downloadFile,
                        mDownLoadProgress.total
                    )
                }
                is DownLoadIntent.DownLoadPauseStateChanged -> {
                    when (uiState.value.downLoadState.value) {
                        DownLoadState.PAUSE -> {
                            handleIntent(DownLoadIntent.BreakpointDownLoadAndSave)
                        }
                        else -> {
                            uiState.value.downLoadState.value = DownLoadState.PAUSE
                        }
                    }
                }
                is DownLoadIntent.DownLoadSaveState -> {
                    when (uiState.value.downLoadState.value) {
                        DownLoadState.LOADING -> {
                            uiState.value.downLoadState.value = DownLoadState.STOP
                            saveDownloadState()
                        }
                        DownLoadState.PAUSE -> {
                            saveDownloadState()
                        }
                        else -> {}
                    }
                }
                is DownLoadIntent.ShowDownLoadState -> {
                    uiState.value.downLoadState.value = DownLoadState.PAUSE
                    uiState.value.downLoadProgress.value = mDownLoadProgress.progress
                }
                is DownLoadIntent.DecompressFile -> {
                    decompressFile()
                }
            }
        }
    }

    private suspend fun downLoadAndSave(url: String, saveFile: String) {
        repository.downLoadAndSave(url, saveFile, isStop = {// 用来控制是否停止下载
            uiState.value.downLoadState.value == DownLoadState.PAUSE || uiState.value.downLoadState.value == DownLoadState.STOP
        }, object : IApiResult<Nothing> {
            override fun downloadProgress(current: Long, total: Long, progress: Float) {
                mDownLoadProgress.update(current = current, total = total, progress = progress)
                uiState.value.downLoadProgress.value = progress
            }
        }).flowOn(Dispatchers.IO).collect {
            it.fold(onSuccess = { file ->
                uiState.value.downloadAppInfo =
                    ZipUtil.getAppInfoByPreDecompressTarGz(file.absolutePath)
                uiState.value.downLoadState.value = DownLoadState.INSTALL
            }, onFailure = {
                uiState.value.downLoadState.value = DownLoadState.FAILURE
                uiState.value.dialogInfo = DialogInfo(
                    DialogType.CUSTOM, title = "异常提示", text = "下载失败! 请联系管理员!", confirmText = "重新下载"
                )
            }, onLoading = {
                uiState.value.downLoadState.value = DownLoadState.LOADING
            }, onPrepare = {
                uiState.value.downLoadState.value = DownLoadState.IDLE
            })
        }
    }

    private suspend fun breakpointDownLoadAndSave(url: String, saveFile: String, total: Long) {
        repository.breakpointDownloadAndSave(url, saveFile, total, isStop = {// 用来控制是否停止下载
            uiState.value.downLoadState.value == DownLoadState.PAUSE || uiState.value.downLoadState.value == DownLoadState.STOP
        }, object : IApiResult<Nothing> {
            override fun downloadProgress(current: Long, total: Long, progress: Float) {
                mDownLoadProgress.update(current = current, total = total, progress = progress)
                uiState.value.downLoadProgress.value = progress
            }
        }).flowOn(Dispatchers.IO).collect {
            it.fold(onSuccess = { file ->
                uiState.value.downloadAppInfo =
                    ZipUtil.getAppInfoByPreDecompressTarGz(file.absolutePath)
                uiState.value.downLoadState.value = DownLoadState.INSTALL
            }, onFailure = {
                uiState.value.downLoadState.value = DownLoadState.FAILURE
                uiState.value.dialogInfo = DialogInfo(
                    DialogType.CUSTOM, title = "异常提示", text = "下载失败! 请联系管理员!", confirmText = "重新下载"
                )
            }, onLoading = {
                uiState.value.downLoadState.value = DownLoadState.LOADING
            }, onPrepare = {
                uiState.value.downLoadState.value = DownLoadState.IDLE
            })
        }
    }

    private suspend fun decompressFile() {
        // 解压文件
        val enableUnzip =
            ZipUtil.decompress(mDownLoadProgress.downloadFile, FilesUtil.getAppUnzipPath())
        if (enableUnzip) {
            uiState.value.downLoadState.value = DownLoadState.COMPLETED
            uiState.value.dialogInfo =
                DialogInfo(DialogType.CUSTOM, title = "提示", text = "下载并安装完成", confirmText = "打开")
        } else {
            uiState.value.downLoadState.value = DownLoadState.FAILURE
            uiState.value.dialogInfo = DialogInfo(
                DialogType.CUSTOM,
                title = "异常提示",
                text = "安装失败! 下载的应用无法正常安装，请联系管理员!",
                confirmText = "重新下载"
            )
        }
    }

    private suspend fun saveDownloadState() {
        repository.saveDownloadState(uiState.value.downLoadState.value, mDownLoadProgress)
    }

    private suspend fun loadDownloadState(path: String) {
        repository.loadDownloadState(path) { downLoadState, downLoadProgress ->
            mDownLoadProgress.copy(downLoadProgress)
            if (mDownLoadProgress.downloadFile.isEmpty()) {
                mDownLoadProgress.downloadFile = FilesUtil.getAppDownloadPath(path)
            }
            mDownLoadState = downLoadState
        }
    }
}
