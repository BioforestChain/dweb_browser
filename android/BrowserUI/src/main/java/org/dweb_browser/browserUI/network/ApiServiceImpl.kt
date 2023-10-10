package org.dweb_browser.browserUI.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.headers
import io.ktor.client.request.prepareGet
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isNotEmpty
import io.ktor.utils.io.core.readBytes
import org.dweb_browser.helper.platform.getKtorClientEngine
import org.dweb_browser.core.std.dns.httpFetch
import java.io.File

class ApiServiceImpl : ApiService {
  private val httpClient = HttpClient(getKtorClientEngine()) {
    install(HttpTimeout) {
      requestTimeoutMillis = 600_000L
    }
  }

  override suspend fun getNetWorker(url: String): String {
    return httpFetch(url).body.toPureString()
  }

  override suspend fun downloadAndSave(
    url: String,
    file: File,
    total: Long,
    isStop: () -> Boolean,
    onProgress: suspend (Long, Long) -> Unit
  ) {
    if (url.isEmpty()) throw (java.lang.Exception("地址有误，下载失败！"))
    onProgress(0L, total)

    httpClient.prepareGet(url).execute { httpResponse ->
      val contentLength = httpResponse.headers["content-length"]?.toLong() ?: total
      var currentLength = 0L
      val channel: ByteReadChannel = httpResponse.body()
      while (!channel.isClosedForRead && !isStop()) {
        val packet = channel.readRemaining(limit = DEFAULT_BUFFER_SIZE.toLong())
        while (packet.isNotEmpty && !isStop()) {
          val bytes: ByteArray = packet.readBytes()
          file.appendBytes(array = bytes)
          currentLength += bytes.size
          onProgress(currentLength, contentLength)
        }
      }
    }
  }

  override suspend fun breakpointDownloadAndSave(
    url: String,
    file: File,
    total: Long,
    isStop: () -> Boolean,
    onProgress: suspend (Long, Long) -> Unit
  ) {
    if (url.isEmpty()) throw (java.lang.Exception("地址有误，下载失败！"))
    var currentLength = if (total > 0) file.length() else 0L // 文件的大小
    onProgress(currentLength, total)

    httpClient.prepareGet(url) {
      headers { set("Range", "bytes=$currentLength-${total}") }
    }.execute { httpResponse ->
      val contentLength = httpResponse.headers["content-length"]?.let {
        currentLength + it.toLong()
      } ?: run {
        currentLength = 0 // 如果没有content-length，说明没办法断点续传，只能重新下载
        total
      } // 网络请求数据的大小
      val channel: ByteReadChannel = httpResponse.body()
      while (!channel.isClosedForRead && !isStop()) {
        val packet = channel.readRemaining(limit = DEFAULT_BUFFER_SIZE.toLong())
        while (packet.isNotEmpty && !isStop()) {
          val bytes: ByteArray = packet.readBytes()
          file.appendBytes(array = bytes)
          currentLength += bytes.size
          onProgress(currentLength, contentLength)
        }
      }
    }
  }
}
