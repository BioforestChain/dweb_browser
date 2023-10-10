package org.dweb_browser.browserUI.network

import info.bagen.dwebbrowser.network.base.checkAndBody
import kotlinx.coroutines.delay
import org.http4k.core.Method
import org.http4k.core.Request
import java.io.File
import java.io.FileOutputStream

class ApiServiceImpl(private val httpClient: HttpClient) : ApiService {

  /*override suspend fun getAppVersion(path: String): ApiResultData<BaseData<AppVersion>> =
    info.bagen.dwebbrowser.network.base.runCatching {
      val type = ParameterizedTypeImpl(BaseData::class.java, arrayOf(AppVersion::class.java))
      gson.fromJson(byteBufferToString(httpClient.requestPath(path).body.payload), type)
    }*/

  override suspend fun getNetWorker(url: String): String {
    return httpClient.requestPath(url).checkAndBody()
  }

  override suspend fun downloadAndSave(
    path: String,
    file: File?,
    total: Long,
    isPause: () -> Boolean,
    isStop: () -> Boolean,
    DLProgress: suspend (Long, Long) -> Unit
  ) {
    if (path.isEmpty()) throw (java.lang.Exception("地址有误，下载失败！"))
    DLProgress(0L, total)
    val httpResponse = httpClient.download(path = path)
    val fileOutputStream: FileOutputStream? = file?.let { FileOutputStream(file) }
    try {
      if (!httpResponse.status.successful) { // 如果网络请求失败，直接抛异常
        throw (java.lang.Exception(httpResponse.status.toString()))
      }

      val contentLength = httpResponse.header("content-length").let {
        it?.toInt() ?: total // 网络请求数据的大小
      }
      val inputStream = httpResponse.body.stream
      var currentLength = 0
      val byteArray = ByteArray(DEFAULT_BUFFER_SIZE)
      var length = inputStream.read(byteArray)
      while (length != -1 && !isStop()) {
        currentLength += length
        fileOutputStream?.write(byteArray, 0, length)
        DLProgress(currentLength.toLong(), contentLength.toLong()) // 将下载进度回调
        while (isPause()) delay(500) // 如果被暂停了，这边循环等待
        length = inputStream.read(byteArray)
      }
      // Log.e("ApiServiceImpl", "downloadAndSave-> $contentLength,$currentLength,${file?.length()}")
    } catch (e: Exception) {
      println("${file?.absoluteFile}->$path, issue[${httpResponse.status}]==>${e.message}")
      DLProgress(-1, httpResponse.status.code.toLong())
    } finally {
      fileOutputStream?.flush()
      fileOutputStream?.close()
    }

    if (isStop()) httpResponse.close()
  }

  @Deprecated("No Support")
  override suspend fun breakpointDownloadAndSave(
    path: String,
    file: File?,
    total: Long,
    isStop: () -> Boolean,
    DLProgress: suspend (Long, Long) -> Unit
  ) {
    if (path.isEmpty()) throw (java.lang.Exception("地址有误，下载失败！"))
    var currentLength = file?.let { if (total > 0) it.length() else 0L } ?: 0L // 文件的大小
    DLProgress(currentLength, total)
    val httpResponse = httpClient.download(path = path) {
      Request(Method.GET, path).header("Range", "bytes=$currentLength-${total}") // 设置获取内容位置
    }

    val fileOutputStream: FileOutputStream? = file?.let { FileOutputStream(file, true) }
    try {
      if (!httpResponse.status.successful) { // 如果网络请求失败，直接抛异常
        throw (java.lang.Exception(httpResponse.status.toString()))
      }

      val contentLength = httpResponse.header("content-length")?.let {
        currentLength + it.toLong()
      } ?: run {
        currentLength = 0 // 如果没有content-length，说明没办法断点续传，只能重新下载
        total
      } // 网络请求数据的大小
      val inputStream = httpResponse.body.stream
      // inputStream.skip(currentLength) // 如果请求时，没有在Head 添加 Range 参数，可以改用这个方法
      val byteArray = ByteArray(DEFAULT_BUFFER_SIZE)
      var length = inputStream.read(byteArray)
      while (length != -1 && !isStop()) {
        currentLength += length
        fileOutputStream?.write(byteArray, 0, length)
        DLProgress(currentLength, contentLength) // 将下载进度回调
        //delay(1000)
        length = inputStream.read(byteArray)
      }
    } catch (e: Exception) {
      println("${file?.absoluteFile}->$path, issue[${httpResponse.status}]==>${e.message}")
      DLProgress(-1, httpResponse.status.code.toLong())
    } finally {
      fileOutputStream?.flush()
      fileOutputStream?.close()
    }
    if (isStop()) httpResponse.close()
  }
}
