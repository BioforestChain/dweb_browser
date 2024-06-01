package org.dweb_browser.browser.web.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import dweb_browser_kmp.browser.generated.resources.Res
import dweb_browser_kmp.browser.generated.resources.ic_download_all
import dweb_browser_kmp.browser.generated.resources.ic_download_audio
import dweb_browser_kmp.browser.generated.resources.ic_download_file
import dweb_browser_kmp.browser.generated.resources.ic_download_image
import dweb_browser_kmp.browser.generated.resources.ic_download_video
import io.ktor.http.ContentType
import io.ktor.http.defaultForFileExtension
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.download.DownloadStateEvent
import org.dweb_browser.browser.download.TaskId
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.createStore
import org.dweb_browser.dwebview.WebDownloadArgs
import org.dweb_browser.helper.Signal
import org.dweb_browser.helper.compose.ObservableMutableState
import org.dweb_browser.helper.compose.SimpleI18nResource
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.helper.globalIoScope
import org.dweb_browser.helper.ioAsyncExceptionHandler
import org.dweb_browser.helper.randomUUID
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
enum class BrowserDownloadType(
  private val contentType: String,
  private val iconRes: DrawableResource,
  private val _title: SimpleI18nResource
) {
  All("all", Res.drawable.ic_download_all, BrowserI18nResource.Download.chip_all),
  Image(
    ContentType.Image.Any.contentType,
    Res.drawable.ic_download_image,
    BrowserI18nResource.Download.chip_image
  ),
  Video(
    ContentType.Video.Any.contentType,
    Res.drawable.ic_download_video,
    BrowserI18nResource.Download.chip_video
  ),
  Audio(
    ContentType.Audio.Any.contentType,
    Res.drawable.ic_download_audio,
    BrowserI18nResource.Download.chip_audio
  ),
  Other(
    ContentType.Application.Any.contentType,
    Res.drawable.ic_download_file,
    BrowserI18nResource.Download.chip_other
  ),
  ;

  @Composable
  fun title() = _title()

  @Composable
  fun painter() = painterResource(iconRes)

  fun matchSuffix(suffix: String): Boolean {
    return ContentType.defaultForFileExtension(suffix).contentType == this.contentType
  }
}

@Serializable
data class BrowserDownloadItem(
  val urlKey: String,
  val downloadArgs: WebDownloadArgs,
  var taskId: TaskId? = null,
  @SerialName("state")
  private var _state: DownloadStateEvent = DownloadStateEvent(),
  var fileName: String = "",
  var fileType: BrowserDownloadType = BrowserDownloadType.All,
  var downloadTime: Long = datetimeNow(), // 记录下载开始时间，等下载完成后，改为下载完成时间。用于排序
  var filePath: String = "", // 用于保存下载文件的路径
) {
  var state by ObservableMutableState(_state) {
    _state = it
    globalIoScope.launch {
      // [iOS] ObservableMutableState: ${it.total} ${it.current}
      stateSignal.emit(it)
    }
  }
  @Transient
  val id = randomUUID() //标识符，iOS端删除时，使用。

  @Transient
  private var stateSignal = Signal<DownloadStateEvent>()

  @Transient
  val stateChanged = stateSignal.toListener()
}

class BrowserDownloadStore(mm: MicroModule.Runtime) {
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