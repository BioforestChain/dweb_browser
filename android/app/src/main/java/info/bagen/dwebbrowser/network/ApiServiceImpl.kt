package info.bagen.dwebbrowser.network

import info.bagen.dwebbrowser.microService.helper.gson
import info.bagen.dwebbrowser.network.base.*
import info.bagen.dwebbrowser.ui.entity.AppVersion
import org.http4k.core.BodyMode.Stream
import org.http4k.core.Request
import java.io.File
import java.io.FileOutputStream

class ApiServiceImpl(private val httpClient: HttpClient) : ApiService {

  override suspend fun getAppVersion(path: String): ApiResultData<BaseData<AppVersion>> =
    info.bagen.dwebbrowser.network.base.runCatching {
      val type = ParameterizedTypeImpl(BaseData::class.java, arrayOf(AppVersion::class.java))
      gson.fromJson(byteBufferToString(httpClient.requestPath(path).body.payload), type)
    }

  override suspend fun getNetWorker(url: String): String {
    return httpClient.requestPath(url).checkAndBody()
  }

  override suspend fun downloadAndSave(
    path: String, file: File?, isStop: () -> Boolean, DLProgress: (Long, Long) -> Unit
  ) {
    if (path.isEmpty()) throw (java.lang.Exception("地址有误，下载失败！"))
    httpClient.requestPath(path = path, bodyMode = Stream)
      .let { httpResponse ->
        val fileOutputStream:FileOutputStream? = file?.let { FileOutputStream(file) }
        try {
          if (!httpResponse.status.successful) { // 如果网络请求失败，直接抛异常
            throw (java.lang.Exception(httpResponse.status.toString()))
          }

          val contentLength = httpResponse.header("content-length").let {
            it?.toInt() ?: 0 // 网络请求数据的大小
          }
          val inputStream = httpResponse.body.stream
          var currentLength = 0
          val byteArray = ByteArray(DEFAULT_BUFFER_SIZE)
          var length = inputStream.read(byteArray)
          while (length != -1 && !isStop()) {
            currentLength += length
            fileOutputStream?.write(byteArray, 0, length)
            DLProgress(currentLength.toLong(), contentLength.toLong()) // 将下载进度回调
            //delay(1000)
            length = inputStream.read(byteArray)
          }
          // Log.e("ApiServiceImpl", "downloadAndSave-> $contentLength,$currentLength,${file?.length()}")
        } catch (e: Exception) {
          e.printStackTrace()
        } finally {
          fileOutputStream?.flush()
          fileOutputStream?.close()
        }

        if (isStop()) httpResponse.close()
      }
  }


  override suspend fun breakpointDownloadAndSave(
    path: String,
    file: File?,
    total: Long,
    isStop: () -> Boolean,
    DLProgress: (Long, Long) -> Unit
  ) {
    if (path.isEmpty()) throw (java.lang.Exception("地址有误，下载失败！"))
    var currentLength = file?.let { if (total > 0) it.length() else 0L } ?: 0L // 文件的大小
    httpClient.requestPath(path = path, bodyMode = Stream) { path, method ->
      Request(method, path).header("Range", "bytes=$currentLength-${total}") // 设置获取内容位置
    }.let { httpResponse ->
      val fileOutputStream: FileOutputStream? = file?.let { FileOutputStream(file, true) }
      try {
        if (!httpResponse.status.successful) { // 如果网络请求失败，直接抛异常
          throw (java.lang.Exception(httpResponse.status.toString()))
        }

        val contentLength = currentLength + httpResponse.header("content-length").let {
          it?.toLong() ?: 0L // 网络请求数据的大小
        }
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
        e.printStackTrace()
      } finally {
        fileOutputStream?.flush()
        fileOutputStream?.close()
      }
      if (isStop()) httpResponse.close()
    }
  }
}
