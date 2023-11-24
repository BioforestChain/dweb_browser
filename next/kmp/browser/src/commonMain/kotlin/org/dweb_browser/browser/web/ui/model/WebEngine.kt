package org.dweb_browser.browser.web.ui.model

import androidx.compose.ui.graphics.ImageBitmap
import io.ktor.http.Url
import io.ktor.http.fullPath
import kotlinx.serialization.Serializable
import org.dweb_browser.browser.BrowserIconResource
import org.dweb_browser.browser.getIconResource

/**
 * 该文件主要定义搜索引擎和引擎默认值，以及配置存储
 */

@Serializable
data class WebEngine(
  val name: String,
  val host: String,
  val start: String,
  var timeMillis: String = "",
  val iconRes: ImageBitmap? = getIconResource(BrowserIconResource.WebEngineDefault),
) {
  fun fit(url: String): Boolean {
    val current = Url("${start}test")
    val query = current.parameters.names().first()
    val uri = Url(url)
    return uri.host == current.host && uri.fullPath == current.fullPath && uri.parameters[query] != null
  }

  fun queryName(): String {
    val current = Url("${start}test")
    return current.parameters.names().first()
  }
}

val DefaultSearchWebEngine: List<WebEngine>
  get() = listOf(
    WebEngine(
      name = "百度",
      host = "m.baidu.com",
      iconRes = getIconResource(BrowserIconResource.WebEngineBaidu),
      start = "https://m.baidu.com/s?word="
    ),
    WebEngine(
      name = "搜狗",
      host = "wap.sogou.com",
      iconRes = getIconResource(BrowserIconResource.WebEngineSougou),
      start = "https://wap.sogou.com/web/searchList.jsp?keyword="
    ),
    WebEngine(
      name = "360",
      host = "m.so.com",
      iconRes = getIconResource(BrowserIconResource.WebEngine360),
      start = "https://m.so.com/s?q="
    ),
  )

val DefaultAllWebEngine: List<WebEngine>
  get() = listOf(
    WebEngine(name = "必应", host = "cn.bing.com", start = "https://cn.bing.com/search?q="),
    WebEngine(name = "百度", host = "m.baidu.com", start = "https://m.baidu.com/s?word="),
    WebEngine(name = "百度", host = "www.baidu.com", start = "https://www.baidu.com/s?wd="),
    WebEngine(
      name = "谷歌", host = "www.google.com", start = "https://www.google.com/search?q="
    ),
    WebEngine(
      name = "搜狗",
      host = "wap.sogou.com",
      start = "https://wap.sogou.com/web/searchList.jsp?keyword="
    ),
    WebEngine(name = "搜狗", host = "www.sogou.com", start = "https://www.sogou.com/web?query="),
    WebEngine(name = "360搜索", host = "m.so.com", start = "https://m.so.com/s?q="),
    WebEngine(name = "360搜索", host = "www.so.com", start = "https://www.so.com/s?q="),
    WebEngine(
      name = "雅虎", host = "search.yahoo.com", start = "https://search.yahoo.com/search?p="
    )
  )

/**
 * 根据内容来判断获取引擎
 */
internal fun findWebEngine(url: String): WebEngine? {
  for (item in DefaultAllWebEngine) {
    if (item.fit(url)) return item
  }
  return null
}