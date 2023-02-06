package info.bagen.libappmgr.network

import info.bagen.libappmgr.entity.AppVersion
import info.bagen.libappmgr.network.base.ApiResultData
import info.bagen.libappmgr.network.base.BaseData
import io.ktor.client.statement.*
import java.io.File

interface ApiService {

    suspend fun getAppVersion(path: String): ApiResultData<BaseData<AppVersion>>
    suspend fun getAppVersion(): BaseData<AppVersion>

    suspend fun download(path: String, onDownload: (Long, Long) -> Unit): HttpResponse
    suspend fun downloadAndSave(
        path: String,
        file: File?,
        isStop: () -> Boolean,
        DLProgress: (Long, Long) -> Unit
    )

    suspend fun getNetWorker(url:String):String

    suspend fun breakpointDownloadAndSave(
        path: String,
        file: File?,
        total: Long,
        isStop: () -> Boolean,
        DLProgress: (Long, Long) -> Unit
    )

    companion object {
        val instance = ApiServiceImpl(KtorManager.apiService)
    }
}
