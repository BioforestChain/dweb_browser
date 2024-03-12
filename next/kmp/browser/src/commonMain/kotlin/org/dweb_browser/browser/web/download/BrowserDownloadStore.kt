package org.dweb_browser.browser.web.download

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.dweb_browser.browser.download.DownloadStateEvent
import org.dweb_browser.browser.download.TaskId
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.createStore
import org.dweb_browser.dwebview.WebDownloadArgs
import org.dweb_browser.helper.compose.ObservableMutableState
import org.dweb_browser.helper.datetimeNow

enum class BrowserDownloadType {
  Application, Picture, Package, Video, Document, Audio, Other;
}

// 文件后缀的类型
enum class FileSuffix(val suffix: String, val type: BrowserDownloadType, val icon: String) {
  ANDROID("apk", BrowserDownloadType.Application, "file:///sys/download/android.svg"),
  IOS("ipa", BrowserDownloadType.Application, "file:///sys/download/ios.svg"),
  EXE("exe", BrowserDownloadType.Application, "file:///sys/download/exe.svg"),
  GZ("gz", BrowserDownloadType.Package, "file:///sys/download/package.svg"),
  ZIP("zip", BrowserDownloadType.Package, "file:///sys/download/package.svg"),
  RAR("rar", BrowserDownloadType.Package, "file:///sys/download/package.svg"),
  DOC("doc", BrowserDownloadType.Document, "file:///sys/download/word.svg"),
  DOCX("doc", BrowserDownloadType.Document, "file:///sys/download/word.svg"),
  XLS("xls", BrowserDownloadType.Document, "file:///sys/download/excel.svg"),
  XLSX("xlsx", BrowserDownloadType.Document, "file:///sys/download/excel.svg"),
  PPT("ppt", BrowserDownloadType.Document, "file:///sys/download/powerpoint.svg"),
  PPTX("pptx", BrowserDownloadType.Document, "file:///sys/download/powerpoint.svg"),
  PDF("pdf", BrowserDownloadType.Document, "file:///sys/download/pdf.svg"),
  PNG("png", BrowserDownloadType.Picture, "file:///sys/download/picture.svg"),
  JPG("jpg", BrowserDownloadType.Picture, "file:///sys/download/picture.svg"),
  JPEG("jpeg", BrowserDownloadType.Picture, "file:///sys/download/picture.svg"),
  BMP("bmp", BrowserDownloadType.Picture, "file:///sys/download/picture.svg"),
  SVG("svg", BrowserDownloadType.Picture, "file:///sys/download/picture.svg"),
  MP3("mp3", BrowserDownloadType.Audio, "file:///sys/download/audio.svg"),
  MP4("mp4", BrowserDownloadType.Video, "file:///sys/download/video.svg"),
  AVI("avi", BrowserDownloadType.Video, "file:///sys/download/video.svg"),
  RMVB("rmvb", BrowserDownloadType.Video, "file:///sys/download/video.svg"),

  Other("*", BrowserDownloadType.Other, "file:///sys/download/file.svg"),
}

@Serializable
data class BrowserDownloadItem(
  val urlKey: String,
  val downloadArgs: WebDownloadArgs,
  var taskId: TaskId? = null,
  @SerialName("state")
  private var _state: DownloadStateEvent = DownloadStateEvent(),
  var fileName: String = "",
  var fileSuffix: FileSuffix = FileSuffix.Other,
  var downloadTime: Long = datetimeNow(), // 记录下载开始时间，等下载完成后，改为下载完成时间。用于排序
) {
  var state by ObservableMutableState(_state) { _state = it }

  @Transient
  var alreadyWatch: Boolean = false
}

class BrowserDownloadStore(mm: MicroModule) {
  private val store = mm.createStore("browser_download", false)
  private val keyOfDownload = "downloading"
  private val keyOfComplete = "completed"

  suspend fun getAll(): MutableMap<String, MutableList<BrowserDownloadItem>> {
    return store.getAll()
  }

  suspend fun save(key: String, items: MutableList<BrowserDownloadItem>) {
    store.set(key, items)
  }

  suspend fun getDownloadAll(): MutableList<BrowserDownloadItem> {
    return store.getOrPut(keyOfDownload) { mutableListOf() }
  }

  suspend fun saveDownloadList(list: MutableList<BrowserDownloadItem>) {
    store.set(keyOfDownload, list)
  }

  suspend fun getCompleteAll(): MutableList<BrowserDownloadItem> {
    return store.getOrPut(keyOfComplete) { mutableListOf() }
  }

  suspend fun saveCompleteList(list: MutableList<BrowserDownloadItem>) {
    store.set(keyOfComplete, list)
  }
}