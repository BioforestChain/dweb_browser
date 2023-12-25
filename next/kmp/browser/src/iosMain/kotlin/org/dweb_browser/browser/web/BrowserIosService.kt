package org.dweb_browser.browser.web

import kotlinx.coroutines.runBlocking
import org.dweb_browser.browser.web.model.WebSiteInfo
import org.dweb_browser.browser.web.model.WebSiteType
import org.dweb_browser.browser.web.ui.model.BrowserViewModel
import org.dweb_browser.helper.platform.NSDataHelper.toByteArray
import org.dweb_browser.helper.platform.NSDataHelper.toNSData
import platform.Foundation.NSData
import platform.UIKit.UIImage

class BrowserIosService(var browserViewModel: BrowserViewModel? = null) {
  //region track mode
  var trackModel: Boolean
    get() {
      val isYes = browserViewModel?.isNoTrace?.value ?: false
      return isYes
    }
    set(value) {
      runBlocking {
        browserViewModel?.saveBrowserMode(value)
      }
    }
  //endregion


  // region desktopLink
  suspend fun createDesktopLink(link: String, title: String, iconString: String) {
    browserViewModel?.addUrlToDesktop(title, link, iconString)
  }
  // endregion

  // region history
  fun loadHistorys(): MutableMap<String, MutableList<WebSiteInfo>>? {
    return browserViewModel?.getHistoryLinks()
  }

  suspend fun loadMoreHistory(off: Int) {
    browserViewModel?.loadMoreHistory(off)
  }

  suspend fun addHistory(title: String, url: String, icon: NSData?) {
    val web =
      WebSiteInfo(title = title, url = url, icon = icon?.toByteArray(), type = WebSiteType.History)
    browserViewModel?.changeHistoryLink(add = web)
  }

  suspend fun removeHistory(history: Long) {
    browserViewModel?.let { vm ->
      try {
        val del = vm.getHistoryLinks().flatMap {
          it.value
        }.first {
          it.id == history
        }
        vm.changeHistoryLink(del = del)
      } catch (e: NoSuchElementException) {
        println("not find")
      }
    }
  }

  // endregion

  // region bookmark
  fun loadBookmarks(): MutableList<WebSiteInfo>? = browserViewModel?.getBookLinks()

  fun addBookmark(title: String, url: String, icon: NSData?) {
    val web =
      WebSiteInfo(title = title, url = url, icon = icon?.toByteArray(), type = WebSiteType.Book)
    browserViewModel?.changeBookLink(web)
  }

  fun removeBookmark(bookmark: Long) {
    browserViewModel?.let { vm ->
      val del = vm.getBookLinks().first {
        it.id == bookmark
      }
      vm.changeBookLink(del = del)
    }
  }

  // endregion


  // region private tools
  fun webSiteInfoIconToUIImage(web: WebSiteInfo): UIImage? = web.icon?.let {
    return UIImage(data = it.toNSData())
  }
  // endregion

}
