package org.dweb_browser.browserUI.database

import android.net.Uri
import androidx.annotation.DrawableRes
import kotlinx.serialization.Serializable
import org.dweb_browser.browserUI.R

/**
 * 该文件主要定义搜索引擎和引擎默认值，以及配置存储
 */

@Serializable
data class WebEngine(
  val name: String,
  val host: String,
  val format: String,
  var timeMillis: String = "",
  @DrawableRes val iconRes: Int = R.drawable.ic_web,
) {
  fun fit(url: String): Boolean {
    val current = Uri.parse(String.format(format, "test"))
    val query = current.queryParameterNames.first()
    val uri = Uri.parse(url)
    return uri.host == current.host && uri.path == current.path && uri.getQueryParameter(query) != null
  }

  fun queryName(): String {
    val current = Uri.parse(String.format(format, "test"))
    return current.queryParameterNames.first()
  }
}

val DefaultSearchWebEngine: List<WebEngine>
  get() = listOf(
    WebEngine(
      name = "百度",
      host = "m.baidu.com",
      iconRes = R.drawable.ic_engine_baidu,
      format = "https://m.baidu.com/s?word=%s"
    ),
    WebEngine(
      name = "搜狗",
      host = "wap.sogou.com",
      iconRes = R.drawable.ic_engine_sougou,
      format = "https://wap.sogou.com/web/searchList.jsp?keyword=%s"
    ),
    WebEngine(
      name = "360",
      host = "m.so.com",
      iconRes = R.drawable.ic_engine_360,
      format = "https://m.so.com/s?q=%s"
    ),
  )

val DefaultAllWebEngine: List<WebEngine>
  get() = listOf(
    WebEngine(name = "必应", host = "cn.bing.com", format = "https://cn.bing.com/search?q=%s"),
    WebEngine(name = "百度", host = "m.baidu.com", format = "https://m.baidu.com/s?word=%s"),
    WebEngine(name = "百度", host = "www.baidu.com", format = "https://www.baidu.com/s?wd=%s"),
    WebEngine(
      name = "谷歌", host = "www.google.com", format = "https://www.google.com/search?q=%s"
    ),
    WebEngine(
      name = "搜狗",
      host = "wap.sogou.com",
      format = "https://wap.sogou.com/web/searchList.jsp?keyword=%s"
    ),
    WebEngine(name = "搜狗", host = "www.sogou.com", format = "https://www.sogou.com/web?query=%s"),
    WebEngine(name = "360搜索", host = "m.so.com", format = "https://m.so.com/s?q=%s"),
    WebEngine(name = "360搜索", host = "www.so.com", format = "https://www.so.com/s?q=%s"),
    WebEngine(
      name = "雅虎", host = "search.yahoo.com", format = "https://search.yahoo.com/search?p=%s"
    )
  )
