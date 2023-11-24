package org.dweb_browser.browser.web.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.createStore
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.helper.datetimeNowToEpochDay
import org.dweb_browser.helper.formatDatestampByEpochDay
import org.dweb_browser.helper.toEpochDay

@Serializable
data class WebSiteInfo(
  val id: Long = datetimeNow(),
  var title: String,
  var url: String = "",
  val type: WebSiteType,
  val timeMillis: Long = id.toEpochDay(),
  @Transient
  val icon: ImageBitmap? = null,
)

@Composable
fun Long.formatToStickyName(): String {
  return when (datetimeNowToEpochDay() - this) {
    0L -> BrowserI18nResource.time_today()
    -1L -> BrowserI18nResource.time_yesterday()
    else -> this.formatDatestampByEpochDay()
  }
}

enum class WebSiteType(val id: Int) {
  History(0), Book(1), Multi(2)
  ;
}

const val KEY_LAST_SEARCH_KEY = "browser.last.keyword"
const val KEY_NO_TRACE = "browser.no.trace" // 无痕浏览

class BrowserStore(mm: MicroModule) {
  private val storeKey = "browser/links"
  private val storeBook = mm.createStore("browser_book", false)
  private val storeHistory = mm.createStore("browser_history", false)
  private val sharePreference = mm.createStore("share_preference", false)

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
    val current = datetimeNowToEpochDay()
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

  suspend fun saveString(key: String, data: String) = sharePreference.set(key, data)
  suspend fun getString(key: String) = sharePreference.getOrNull<String>(key)
}