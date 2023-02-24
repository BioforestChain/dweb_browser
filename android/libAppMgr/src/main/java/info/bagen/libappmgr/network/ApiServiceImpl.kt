package info.bagen.libappmgr.network

import com.google.gson.Gson
import info.bagen.libappmgr.entity.AppVersion
import info.bagen.libappmgr.network.base.*
import org.http4k.core.BodyMode.Stream
import org.http4k.core.Request
import java.io.File

class ApiServiceImpl(private val httpClient: HttpClient) : ApiService {

  override suspend fun getAppVersion(path: String): ApiResultData<BaseData<AppVersion>> =
    info.bagen.libappmgr.network.base.runCatching {
      val type = ParameterizedTypeImpl(BaseData::class.java, arrayOf(AppVersion::class.java))
      Gson().fromJson(byteBufferToString(httpClient.requestPath(path).body.payload), type)
    }

override suspend fun getNetWorker(url: String): String {
  return httpClient.requestPath(url).checkAndBody()
}

override suspend fun downloadAndSave(
  path: String, file: File?, isStop: () -> Boolean, DLProgress: (Long, Long) -> Unit
) {
  if (path.isEmpty()) throw(java.lang.Exception("地址有误，下载失败！"))
  httpClient.requestPath(path = path, bodyMode = Stream)
    .let { httpResponse ->
      try {
        if (!httpResponse.status.successful) { // 如果网络请求失败，直接抛异常
          throw(java.lang.Exception(httpResponse.status.toString()))
        }

        val contentLength = httpResponse.header("content-length").let {
          it?.toInt() ?: 0 // 网络请求数据的大小
        }
        val inputStream = httpResponse.body.stream
        var currentLength = 0
        var byteArray = inputStream.readNBytes(DEFAULT_BUFFER_SIZE)
        while (byteArray != null && byteArray.isNotEmpty() && !isStop()) {
          //val byteArray = packet.readBytes()
          currentLength += byteArray.size
          file?.appendBytes(byteArray)
          DLProgress(currentLength.toLong(), contentLength.toLong()) // 将下载进度回调
          byteArray = inputStream.readNBytes(DEFAULT_BUFFER_SIZE)
          //packet = inputStream.readPacketAtMost(DEFAULT_BUFFER_SIZE.toLong())
        }
        // Log.e("ApiServiceImpl", "downloadAndSave-> $contentLength,$currentLength,${file?.length()}")
      } catch (e: Exception) {
        e.printStackTrace()
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
  if (path.isEmpty()) throw(java.lang.Exception("地址有误，下载失败！"))
  var currentLength = file?.let { if (total > 0) it.length() else 0L } ?: 0L // 文件的大小
  httpClient.requestPath(path = path, bodyMode = Stream) { path, method ->
    Request(method, path).header("Range", "bytes=$currentLength-${total}") // 设置获取内容位置
  }.let { httpResponse ->
    try {
      if (!httpResponse.status.successful) { // 如果网络请求失败，直接抛异常
        throw(java.lang.Exception(httpResponse.status.toString()))
      }

      val contentLength = currentLength + httpResponse.header("content-length").let {
        it?.toLong() ?: 0L // 网络请求数据的大小
      }
      // Log.e("ApiServiceImpl", "breakpointDownloadAndSave-> $contentLength,$currentLength,${file?.length()}")
      val inputStream = httpResponse.body.stream
      // inputStream.skip(currentLength) // 如果请求时，没有在Head 添加 Range 参数，可以改用这个方法
      //var packet = inputStream.readPacketAtMost(DEFAULT_BUFFER_SIZE.toLong())
      val byteArray = inputStream.readNBytes(DEFAULT_BUFFER_SIZE)
      while (byteArray != null && byteArray.isNotEmpty() && !isStop()) {
        //val byteArray = packet.readBytes()
        currentLength += byteArray.size
        file?.appendBytes(byteArray)
        DLProgress(currentLength, contentLength) // 将下载进度回调
        //packet = inputStream.readPacketAtMost(DEFAULT_BUFFER_SIZE.toLong())
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    if (isStop()) httpResponse.close()
  }
}
}
