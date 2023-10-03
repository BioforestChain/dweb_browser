package org.dweb_browser.microservice.sys.download

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.prepareGet
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isNotEmpty
import io.ktor.utils.io.core.readBytes
import org.dweb_browser.helper.platform.getKtorClientEngine
import java.io.File

object HttpDownload {
  private val httpClient = HttpClient(getKtorClientEngine()) {
    install(HttpTimeout) {
      requestTimeoutMillis = 600_000L
    }
  }

  suspend fun downloadAndSave(
    downloadInfo: JmmDownloadInfo, isStop: () -> Boolean, onProgress: (Long, Long) -> Unit
  ) {
    onProgress(0L, downloadInfo.size)
    val file = File(downloadInfo.path)

    httpClient.prepareGet(downloadInfo.url).execute { httpResponse ->
      val contentLength = httpResponse.headers["content-length"]?.toLong() ?: downloadInfo.size
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
}