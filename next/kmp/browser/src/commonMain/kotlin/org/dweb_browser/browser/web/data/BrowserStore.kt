package org.dweb_browser.browser.web.data

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.browser.web.model.WebEngine
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.createStore
import org.dweb_browser.helper.datetimeNow
import org.dweb_browser.helper.datetimeNowToEpochDay
import org.dweb_browser.helper.formatDatestampByEpochDay
import org.dweb_browser.helper.platform.toImageBitmap
import org.dweb_browser.helper.toEpochDay

@Serializable
data class WebSiteInfo(
  val id: Long = datetimeNow(),
  var title: String,
  var url: String = "",
  val type: WebSiteType,
  val timeMillis: Long = id.toEpochDay(),
  var icon: ByteArray? = null,
) {
  @Transient
  var iconImage: ImageBitmap? = null
    get() = icon?.toImageBitmap() ?: null
}

@Composable
fun Long.formatToStickyName(): String {
  return when (this - datetimeNowToEpochDay()) {
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
  private val storeBookKey = "browser/links"
  private val storeEngineKey = "SearchEngines"
  private val storeBook = mm.createStore("browser_book", false)
  private val storeHistory = mm.createStore("browser_history", false)
  private val storeEngines = mm.createStore("search_engine", false)
  private val sharePreference = mm.createStore("share_preference", false)

  /**
   * 书签部分，不需要特殊处理，直接保存即可
   */
  suspend fun getBookLinks() = storeBook.getOrPut(storeBookKey) {
    return@getOrPut mutableListOf<WebSiteInfo>()
  }

  suspend fun setBookLinks(data: MutableList<WebSiteInfo>) =
    storeBook.set(storeBookKey, data)

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

  /**
   * 取下一个7天的历史数据
   * off: 到今天的偏移天数
   */
  suspend fun getDaysHistoryLinks(off: Int): MutableMap<String, MutableList<WebSiteInfo>> {
    val current = datetimeNowToEpochDay() + off
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

  suspend fun setSearchEngines(data: MutableList<WebEngine>) =
    storeEngines.set(storeEngineKey, data)

  suspend fun getSearchEngines() = storeEngines.getOrPut(storeEngineKey) {
    mutableListOf<WebEngine>()
  }
}