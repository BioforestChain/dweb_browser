package info.bagen.libappmgr.ui.download

import androidx.collection.arraySetOf
import info.bagen.libappmgr.network.ApiService
import info.bagen.libappmgr.network.base.ApiResultData
import info.bagen.libappmgr.network.base.IApiResult
import info.bagen.libappmgr.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

class DownLoadRepository(private val api: ApiService = ApiService.instance) {

    suspend fun downLoadAndSave(
        url: String, saveFile: String, isStop: () -> Boolean, apiResult: IApiResult<Nothing>
    ): Flow<ApiResultData<File>> = flow {
        // emit(ApiResultData.prepare()) // 通知修改状态为准备
        try {
            emit(ApiResultData.progress()) // 通知修改状态为加载
            var file = File(saveFile)
            var contentLength = 0L
            api.downloadAndSave(url, file, isStop) { current, total ->
                contentLength = total
                val progress = current.toFloat() / (total)
                apiResult.downloadProgress(current, total, progress) // 进度需要单独处理，因为emit必须在协程中调用
            }
            if (file.length() == contentLength) {
                delay(1000)
                emit(ApiResultData.success(file))
            }
        } catch (e: Exception) {
            emit(ApiResultData.failure(e))
        }
    }

    /**
     * 实现断点下载功能
     */
    suspend fun breakpointDownloadAndSave(
        url: String,
        saveFile: String,
        total: Long,
        isStop: () -> Boolean,
        apiResult: IApiResult<Nothing>
    ): Flow<ApiResultData<File>> = flow {
        // emit(ApiResultData.prepare()) // 通知修改状态为准备
        try {
            emit(ApiResultData.progress()) // 通知修改状态为加载
            val file = File(saveFile)
            var contentLength = 0L
            api.breakpointDownloadAndSave(url, file, total, isStop = isStop) { current, total ->
                contentLength = total
                val progress = current.toFloat() / (total)
                apiResult.downloadProgress(current, total, progress) // 进度需要单独处理，因为emit必须在协程中调用
            }
            if (file.length() == contentLength) {
                delay(1000)
                emit(ApiResultData.success(file))
            }
        } catch (e: Exception) {
            emit(ApiResultData.failure(e))
        }
    }

    /**
     * 将当前下载的信息进行存储，可以是 SharePreference 也可以是数据库
     */
    suspend fun saveDownloadState(
        downLoadState: DownLoadState, downLoadProgress: DownLoadProgress
    ) {
        AppContextUtil.sInstance?.let { context ->
            val value = arraySetOf(downLoadState.name, JsonUtil.toJson(downLoadProgress))
            context.saveStringSet("download_${downLoadProgress.downloadUrl}", value)
        }
    }

    /**
     * 从之前保存的下载信息中获取下载状态，判断是否需要恢复
     */
    suspend fun loadDownloadState(
        path: String,
        callback: (DownLoadState, DownLoadProgress) -> Unit
    ) {
        AppContextUtil.sInstance?.let { context ->
            val downLoad = context.getStringSet("download_$path")
            val downloadFile = FilesUtil.getAppDownloadPath(path)
            var downloadState = DownLoadState.IDLE
            var downloadProgress = DownLoadProgress(downloadUrl = path, downloadFile = downloadFile)
            downLoad?.let { setString ->
                setString.forEachIndexed { index: Int, s: String ->
                    if (index == 0 && s.isNotEmpty()) {
                        downloadState = DownLoadState.valueOf(s)
                    } else if (index == 1 && s.isNotEmpty()) {
                        JsonUtil.fromJson(DownLoadProgress::class.java, s)?.let {
                            it.downloadUrl = it.downloadUrl.ifEmpty { path }
                            it.downloadFile = it.downloadFile.ifEmpty { downloadFile }
                            downloadProgress = it
                        }
                    }
                }
            }
            callback(downloadState, downloadProgress)
            context.remove(key = "download_$path")
        }
    }
}
