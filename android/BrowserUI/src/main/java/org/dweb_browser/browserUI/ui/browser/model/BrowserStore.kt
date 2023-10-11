package org.dweb_browser.browserUI.ui.browser.model

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.serialization.Serializable
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.createStore
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Serializable
data class WebSiteInfo(
  val id: Long = System.currentTimeMillis(),
  var title: String,
  var url: String = "",
  val type: WebSiteType,
  val timeMillis: Long = LocalDate.now().toEpochDay(),
  val icon: ImageBitmap? = null,
) {
  fun getStickyName(): String {
    val currentOfEpochDay = LocalDate.now().toEpochDay()
    return if (timeMillis >= currentOfEpochDay) {
      "今天"
    } else if (timeMillis == currentOfEpochDay - 1) {
      "昨天"
    } else {
      LocalDate.ofEpochDay(timeMillis).format(DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE"))
    }
  }
}

fun Long.formatToStickyName(): String {
  val currentOfEpochDay = LocalDate.now().toEpochDay()
  return if (this >= currentOfEpochDay) {
    "今天"
  } else if (this == currentOfEpochDay - 1) {
    "昨天"
  } else {
    LocalDate.ofEpochDay(this).format(DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE"))
  }
}

enum class WebSiteType(val id: Int) {
  History(0), Book(1), Multi(2)
  ;
}

class BrowserStore(mm: MicroModule) {
  private val storeKey = "browser/links"
  private val storeBook = mm.createStore("browser_book", false)
  private val storeHistory = mm.createStore("browser_history", false)

  /**
   * 书签部分，不需要特殊处理，直接保存即可
   */
  suspend fun getBookLinks() = storeBook.getOrPut(storeKey) {
    return@getOrPut mutableListOf<WebSiteInfo>()
  }

  suspend fun setBookLinks(data: MutableList<WebSiteInfo>) =
    storeBook.set(storeKey, data)

  /**
   * 历史部分，需要特殊处理
   * 保存的时候：按照每天保存一份文件
   * 获取的时候：按照每天的map来获取最近7天的数据
   */
  suspend fun getHistoryLinks(): MutableMap<String, MutableList<WebSiteInfo>> {
    val current = LocalDate.now().toEpochDay()
    val maps = mutableMapOf<String, MutableList<WebSiteInfo>>()
    for (day in current downTo current - 6) { // 获取最近一周的数据
      val webSiteInfoList = storeHistory.getOrNull<MutableList<WebSiteInfo>>(day.toString())
      if (webSiteInfoList?.isNotEmpty() == true) {
        maps[day.toString()] = webSiteInfoList
      }
    }
    return maps
  }

  suspend fun setHistoryLinks(key: String, data: MutableList<WebSiteInfo>) {
    storeHistory.set(key, data)
  }
}