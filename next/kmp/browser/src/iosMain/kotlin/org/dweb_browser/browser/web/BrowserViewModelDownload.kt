package org.dweb_browser.browser.web

import org.dweb_browser.browser.download.DownloadStateEvent
import org.dweb_browser.browser.web.data.BrowserDownloadItem
import org.dweb_browser.browser.web.model.BrowserViewModel
import org.dweb_browser.helper.OffListener
import org.dweb_browser.helper.UUID

private var downloadProgressListener = mutableMapOf<UUID, OffListener<DownloadStateEvent>>()

class BrowserViewModelDownloadImplementor(browserViewModel: BrowserViewModel) :
  BrowserViewModelIosImplementor(
    browserViewModel
  ) {

  val allDownloadList: List<BrowserDownloadItem>
    get() = downloadCompletedList + downloadingList

  private val downloadCompletedList: List<BrowserDownloadItem>
    get() = browserViewModel.browserController.downloadController.completeList

  private val downloadingList: List<BrowserDownloadItem>
    get() = browserViewModel.browserController.downloadController.downloadList

  suspend fun resumeDownload(id: UUID) {
    downloadingList.firstOrNull {
      it.id == id
    }?.let {
      browserViewModel.browserController.downloadController.startDownload(it)
    }
  }

  suspend fun pauseDownload(id: UUID) {
    downloadingList.firstOrNull {
      it.id == id
    }?.let {
      browserViewModel.browserController.downloadController.pauseDownload(it)
    }
  }

  fun addDownloadProgressListenerIfNeed(id: UUID, action: (BrowserDownloadItem) -> Unit) {
    downloadingList.firstOrNull {
      it.id == id && !downloadProgressListener.containsKey(id)
    }?.let { downloadItem ->
      val off = downloadItem.stateChanged { state ->
        action(downloadItem)
      }
      downloadProgressListener.put(downloadItem.id, off)
    }
  }

  fun removeDownloadProgressListenerIfNeed(id: UUID) {
    downloadProgressListener.remove(id)?.let {
      it()
    }
  }

  fun removeAllDonwloadProgressListener() {
    downloadProgressListener.all {
      it.value()
    }
    downloadProgressListener.clear()
  }

  fun deletedDonwload(ids: List<String>) {
    allDownloadList.filter {
      ids.contains(it.id)
    }?.let {
      browserViewModel.browserController.downloadController.deleteDownloadItems(it.toMutableList())
    }
  }

  fun getDownloadLocalPath(taskId: String): String? {
    return null
  }

  override fun destory() {
    removeAllDonwloadProgressListener()
  }
}