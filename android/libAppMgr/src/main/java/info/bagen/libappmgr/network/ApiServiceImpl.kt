package info.bagen.libappmgr.network

import info.bagen.libappmgr.entity.AppVersion
import info.bagen.libappmgr.network.base.ApiResultData
import info.bagen.libappmgr.network.base.BaseData
import info.bagen.libappmgr.network.base.bodyData
import info.bagen.libappmgr.network.base.checkAndBody
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.cancel
import java.io.File

class ApiServiceImpl(private val client: HttpClient) : ApiService {


    override suspend fun getAppVersion(path: String): ApiResultData<BaseData<AppVersion>> =
        info.bagen.libappmgr.network.base.runCatching {
            client.get(path).checkAndBody()
        }

    override suspend fun getAppVersion(): BaseData<AppVersion> =
        client.get("KEJPMHLA/appversion.json").bodyData()

    override suspend fun download(path: String, DLProgress: (Long, Long) -> Unit): HttpResponse =
        client.get(path) {
            onDownload { bytesSentTotal, contentLength ->
                DLProgress(bytesSentTotal, contentLength)
            }
        }

    override suspend fun downloadAndSave(
        path: String, file: File?, isStop: () -> Boolean, DLProgress: (Long, Long) -> Unit
    ) {
        if (path.isEmpty()) throw(java.lang.Exception("地址有误，下载失败！"))
        client.prepareGet(path).execute { httpResponse ->
            if (!httpResponse.status.isSuccess()) { // 如果网络请求失败，直接抛异常
                throw(java.lang.Exception(httpResponse.status.toString()))
            }
            val channel: ByteReadChannel = httpResponse.body()
            val contentLength = httpResponse.contentLength() // 网络请求数据的大小
            var currentLength = 0L
            var isStop = isStop()

            while (!channel.isClosedForRead && !isStop) {
                val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                while (!packet.isEmpty) {
                    val bytes = packet.readBytes()
                    currentLength += bytes.size
                    file?.appendBytes(bytes)
                    DLProgress(currentLength, contentLength!!) // 将下载进度回调
                }
                isStop = isStop()
            }
            if (isStop) httpResponse.cancel()
        }
    }

    override suspend fun getNetWorker(url: String): String {
       val response =  client.get(url)
       return response.bodyAsText()
    }

    override suspend fun breakpointDownloadAndSave(
        path: String,
        file: File?,
        total: Long,
        isStop: () -> Boolean,
        DLProgress: (Long, Long) -> Unit
    ) {
        if (path.isEmpty()) throw(java.lang.Exception("地址有误，下载失败！"))

        var currentLength = file?.let { if (total > 0) it.length() else 0L } ?: 0L // 文件的大小

        // 判断当前地址获取的大小跟之前下载的大小是否一致，如果不一致，直接重新下载
        client.prepareGet(path).execute {
            val length = it.contentLength() ?: 0L
            if (length != total) currentLength = 0L
        }

        client.prepareGet(path) {
            if (currentLength > 0) {
                this.header("Range", "bytes=$currentLength-${total}") // 设置获取内容位置
            }
        }.execute { httpResponse ->
            if (!httpResponse.status.isSuccess()) { // 如果网络请求失败，直接抛异常
                throw(java.lang.Exception(httpResponse.status.toString()))
            }
            val channel: ByteReadChannel = httpResponse.body()
            val contentLength =
                (httpResponse.contentLength() ?: 0L) + currentLength // 网络请求数据的大小，由于请求时扣减了。所以这边手动补上
            var isStop = isStop()

            while (!channel.isClosedForRead && !isStop) {
                val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                while (!packet.isEmpty) {
                    val bytes = packet.readBytes()
                    currentLength += bytes.size
                    file?.appendBytes(bytes)
                    DLProgress(currentLength, contentLength!!) // 将下载进度回调
                }
                isStop = isStop()
            }
            if (isStop) httpResponse.cancel()
        }
    }
}
