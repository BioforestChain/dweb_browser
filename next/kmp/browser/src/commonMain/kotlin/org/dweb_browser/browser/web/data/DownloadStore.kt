package org.dweb_browser.browser.web.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import dweb_browser_kmp.browser.generated.resources.Res
import dweb_browser_kmp.browser.generated.resources.ic_download_android
import dweb_browser_kmp.browser.generated.resources.ic_download_audio
import dweb_browser_kmp.browser.generated.resources.ic_download_excel
import dweb_browser_kmp.browser.generated.resources.ic_download_exe
import dweb_browser_kmp.browser.generated.resources.ic_download_file
import dweb_browser_kmp.browser.generated.resources.ic_download_ios
import dweb_browser_kmp.browser.generated.resources.ic_download_package
import dweb_browser_kmp.browser.generated.resources.ic_download_pdf
import dweb_browser_kmp.browser.generated.resources.ic_download_picture
import dweb_browser_kmp.browser.generated.resources.ic_download_powerpoint
import dweb_browser_kmp.browser.generated.resources.ic_download_video
import dweb_browser_kmp.browser.generated.resources.ic_download_word
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.dweb_browser.browser.download.DownloadStateEvent
import org.dweb_browser.browser.download.TaskId
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.createStore
import org.dweb_browser.dwebview.WebDownloadArgs
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.UUID
import org.dweb_browser.helper.compose.ObservableMutableState
import org.dweb_browser.helper.datetimeNow
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.dweb_browser.helper.randomUUID

enum class BrowserDownloadType {
  Application, Picture, Package, Video, Document, Audio, Other;
}

// 文件后缀的类型
@OptIn(ExperimentalResourceApi::class)
enum class FileSuffix(
  val suffix: String,
  val type: BrowserDownloadType,
  val icon: DrawableResource
) {
  ANDROID("apk", BrowserDownloadType.Application, Res.drawable.ic_download_android),
  IOS("ipa", BrowserDownloadType.Application, Res.drawable.ic_download_ios),
  EXE("exe", BrowserDownloadType.Application, Res.drawable.ic_download_exe),
  GZ("gz", BrowserDownloadType.Package, Res.drawable.ic_download_package),
  ZIP("zip", BrowserDownloadType.Package, Res.drawable.ic_download_package),
  RAR("rar", BrowserDownloadType.Package, Res.drawable.ic_download_package),
  DOC("doc", BrowserDownloadType.Document, Res.drawable.ic_download_word),
  DOCX("doc", BrowserDownloadType.Document, Res.drawable.ic_download_word),
  XLS("xls", BrowserDownloadType.Document, Res.drawable.ic_download_excel),
  XLSX("xlsx", BrowserDownloadType.Document, Res.drawable.ic_download_excel),
  PPT("ppt", BrowserDownloadType.Document, Res.drawable.ic_download_powerpoint),
  PPTX("pptx", BrowserDownloadType.Document, Res.drawable.ic_download_powerpoint),
  PDF("pdf", BrowserDownloadType.Document, Res.drawable.ic_download_pdf),
  PNG("png", BrowserDownloadType.Picture, Res.drawable.ic_download_picture),
  JPG("jpg", BrowserDownloadType.Picture, Res.drawable.ic_download_picture),
  JPEG("jpeg", BrowserDownloadType.Picture, Res.drawable.ic_download_picture),
  BMP("bmp", BrowserDownloadType.Picture, Res.drawable.ic_download_picture),
  SVG("svg", BrowserDownloadType.Picture, Res.drawable.ic_download_picture),
  MP3("mp3", BrowserDownloadType.Audio, Res.drawable.ic_download_audio),
  MP4("mp4", BrowserDownloadType.Video, Res.drawable.ic_download_video),
  AVI("avi", BrowserDownloadType.Video, Res.drawable.ic_download_video),
  RMVB("rmvb", BrowserDownloadType.Video, Res.drawable.ic_download_video),
  WMV("wmv", BrowserDownloadType.Video, Res.drawable.ic_download_video),

  Other("*", BrowserDownloadType.Other, Res.drawable.ic_download_file),
  ;

  @Composable
  fun painter() = painterResource(icon)
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
  var filePath: String = "", // 用于保存下载文件的路径
) {
  var state by ObservableMutableState(_state) {
    _state = it
    CoroutineScope(Dispatchers.IO).launch {
      println("[iOS] ObservableMutableState: ${it.total} ${it.current}")
      stateSignal.emit(it)
    }
  }
  val id = randomUUID() //标识符，iOS端删除时，使用。

  @Transient
  var alreadyWatch: Boolean = false

  @Transient
  private var stateSignal = Signal<DownloadStateEvent>()
  @Transient
  val stateChanged = stateSignal.toListener()
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