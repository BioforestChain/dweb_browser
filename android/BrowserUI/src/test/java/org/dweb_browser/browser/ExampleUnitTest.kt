package org.dweb_browser.browser

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.url
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.now
import org.dweb_browser.helper.padEndAndSub
import org.dweb_browser.helper.runBlockingCatching
import org.junit.Assert.assertEquals
import org.junit.Test

fun debugUnitTest(tag: String, msg: Any? = "", err: Throwable? = null) {
  val scopeTag = "${now()} │ ${"unitTest".padEndAndSub(16)} │ ${tag.padEndAndSub(22)} |"
  println("${scopeTag.padEnd(60, ' ')} $msg")
  err?.printStackTrace()
}

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun downloadTest() {
    debugUnitTest("downloadTest", "开始")
    val httpClient = HttpClient(CIO) {
      //install(HttpCache)
      install(HttpTimeout) {
        //requestTimeoutMillis = 300_000L
        connectTimeoutMillis = 30_000L
      }
    }

    runBlockingCatching(ioAsyncExceptionHandler) {
      debugUnitTest("downloadTest", "准备下载")
      httpClient.prepareRequest {
        //url("https://dweb.waterbang.top/game.dweb.waterbang.top.dweb-1.1.1.zip")
        //url("http://linge.plaoc.com/BaiduNetdisk_7.22.0.8.exe")
        url("http://linge.plaoc.com/Clash.zip")
      }.execute { httpResponse ->
        debugUnitTest("downloadTest", "正在下载...")
        val channel: ByteReadChannel = httpResponse.body()
        var current = 0L
        while (!channel.isClosedForRead) {
          val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
          while (!packet.isEmpty) {
            val bytes = packet.readBytes()
            current += bytes.size
            debugUnitTest("downloadTest", "下载量：$current -> 总量：${httpResponse.contentLength()}")
          }
        }
        debugUnitTest("downloadTest", "已下载完成")
      }
    }
    debugUnitTest("downloadTest", "测试结束")
  }
}