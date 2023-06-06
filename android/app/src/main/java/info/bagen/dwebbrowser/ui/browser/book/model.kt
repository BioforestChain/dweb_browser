package info.bagen.dwebbrowser.ui.browser.book

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.bagen.dwebbrowser.database.WebSiteDatabase
import info.bagen.dwebbrowser.database.WebSiteInfo
import info.bagen.dwebbrowser.database.WebSiteType
import info.bagen.dwebbrowser.microService.helper.ioAsyncExceptionHandler
import info.bagen.dwebbrowser.microService.helper.mainAsyncExceptionHandler
import kotlinx.coroutines.launch


class BookViewModel : ViewModel() {
  val bookList: MutableList<WebSiteInfo> = mutableStateListOf()
  var currentWebSiteInfo: WebSiteInfo? = null

  init {
    viewModelScope.launch(mainAsyncExceptionHandler) {
      WebSiteDatabase.INSTANCE.websiteDao().loadAllByTypeAscObserve(WebSiteType.Book)
        .observeForever {
          bookList.clear()
          it.forEach { webSiteInfo ->
            bookList.add(webSiteInfo)
          }
        }
    }
  }

  fun deleteWebSiteInfo(webSiteInfo: WebSiteInfo) {
    bookList.remove(webSiteInfo)
    viewModelScope.launch(ioAsyncExceptionHandler) {
      WebSiteDatabase.INSTANCE.websiteDao().delete(webSiteInfo)
    }
  }
}


val LocalBookViewModel = compositionLocalOf { BookViewModel() }