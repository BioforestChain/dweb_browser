package org.dweb_browser.browser.download

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.prepareGet
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isNotEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.delay
import org.dweb_browser.helper.platform.getKtorClientEngine
import java.io.File

object HttpDownload {
  private val httpClient = HttpClient(getKtorClientEngine()) {
    install(HttpTimeout) {
      requestTimeoutMillis = 600_000L
    }
  }

  suspend fun downloadAndSave(
    downloadInfo: DownloadTask,
    isStop: () -> Boolean,
    isPause: () -> Boolean,
    onProgress: (Long, Long) -> Unit
  ) {
    onProgress(0L, downloadInfo.status.total)
    val file = File(downloadInfo.filepath)

    httpClient.prepareGet(downloadInfo.url).execute { httpResponse ->
      val contentLength = httpResponse.headers["content-length"]?.toLong() ?: downloadInfo.status.total
      var currentLength = 0L
      val channel: ByteReadChannel = httpResponse.body()
      while (!channel.isClosedForRead) {
        val packet = channel.readRemaining(limit = DEFAULT_BUFFER_SIZE.toLong())
        while (packet.isNotEmpty && !isStop()) {
          while (isPause()) delay(500)
          val bytes: ByteArray = packet.readBytes()
          file.appendBytes(array = bytes)
          currentLength += bytes.size
          onProgress(currentLength, contentLength)
        }
      }
    }
  }
}