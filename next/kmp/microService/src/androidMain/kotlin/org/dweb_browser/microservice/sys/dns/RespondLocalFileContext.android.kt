package org.dweb_browser.microservice.sys.dns

import android.content.res.AssetManager
import io.ktor.util.cio.toByteReadChannel
import org.dweb_browser.microservice.http.PureResponse
import java.io.File
import java.io.InputStream

fun RespondLocalFileContext.returnFile(inputStream: InputStream) =
  returnFile(inputStream.toByteReadChannel())

fun RespondLocalFileContext.returnFile(file: File) =
  if (preferenceStream) returnFile(file.inputStream()) else returnFile(file.readBytes())

fun String.parseToDirnameAndBasename(): Pair<String, String> {
  var basename = ""
  var dirname = ""
  val pathSegments = trim('/').split('/').filter { it.isNotEmpty() }.toMutableList()
  when (pathSegments.size) {
    0 -> {}
    1 -> {
      dirname = ""
      basename = pathSegments.first()
    }

    else -> {
      basename = pathSegments.removeLast()
      dirname = pathSegments.joinToString("/")
    }
  }
  return Pair(dirname, basename)
}

fun RespondLocalFileContext.returnAndroidAsset(
  assetManager: AssetManager,
  assetPath: String = filePath
): PureResponse {
  val (dirname, basename) = assetPath.parseToDirnameAndBasename()
  /// 尝试打开文件，如果打开失败就走 404 no found 响应
  val filenameList = assetManager.list(dirname) ?: emptyArray()

  if (!filenameList.contains(basename)) {
    return returnNoFound()
  }

  val file = assetManager.open(
    "$dirname/$basename",
    if (preferenceStream) AssetManager.ACCESS_STREAMING else AssetManager.ACCESS_BUFFER
  )
  return returnFile(file)
}

fun RespondLocalFileContext.returnAndroidFile(
  root: String,
  filePath: String = this.filePath
): PureResponse {
  val fullFilePath = root + File.separator + filePath.trimStart('/')
  return try {
    returnFile(File(fullFilePath))
  } catch (e: Throwable) {
    returnNoFound(e.message)
  }
}
