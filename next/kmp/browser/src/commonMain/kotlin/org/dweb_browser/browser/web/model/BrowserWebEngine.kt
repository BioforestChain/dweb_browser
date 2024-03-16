package org.dweb_browser.browser.web.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import io.ktor.http.Url
import io.ktor.http.fullPath
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.dweb_browser.browser.BrowserDrawResource
import org.dweb_browser.helper.compose.ObservableMutableState
import org.dweb_browser.pure.image.compose.PureImageLoader


/**
 * 该文件主要定义搜索引擎和引擎默认值，以及配置存储
 */
@Serializable
data class WebEngine(
  @SerialName("name") private var _name: String,
  val host: String,
  val start: String,
  val timeMillis: String = "",
  val icon: String? = null,
  @SerialName("checked") private var _checked: Boolean = false,
) {
  var checked by ObservableMutableState(_checked) { _checked = it }
  var name by ObservableMutableState(_name) { _name = it }

  @Composable
  fun iconPainter(): Painter = key(icon) {
    icon?.let {
      // 尝试读取静态文件
      BrowserDrawResource.ALL_VALUES[it]?.painter() ?:
      // 使用网络记载
      PureImageLoader.SmartLoad(url = it, 64.dp, 64.dp).painter()
    }
  } ?: BrowserDrawResource.Web.painter()

  fun fit(url: String): Boolean {
    val current = Url("${start}test")
    val query = current.parameters.names().first()
    val uri = Url(url)
    return uri.host == current.host && uri.fullPath == current.fullPath && uri.parameters[query] != null
  }

  fun queryName(): String {
    val current = Url("${start}test")
    return current.parameters.names().last()
  }

  override fun toString(): String {
    return "WebEngine(name=$name, host=$host, start=$start, timeMillis=$timeMillis, checked=$_checked)"
  }
}

val DefaultSearchWebEngine = listOf(
  WebEngine(
    _name = "百度",
    host = "m.baidu.com",
    icon = BrowserDrawResource.WebEngineBaidu.id,
    start = "https://m.baidu.com/s?word="
  ),
  /*WebEngine(
    _name = "必应",
    host = "cn.bing.com",
    icon = BrowserIconResource.WebEngineBing.id,
    start = "https://cn.bing.com/search?q="
  ),*/
  WebEngine(
    _name = "搜狗",
    host = "wap.sogou.com",
    icon = BrowserDrawResource.WebEngineSogou.id,
    start = "https://wap.sogou.com/web/searchList.jsp?keyword="
  ),
  WebEngine(
    _name = "360",
    host = "m.so.com",
    icon = BrowserDrawResource.WebEngine360.id,
    start = "https://m.so.com/s?q="
  ),
)

val DefaultAllWebEngine: List<WebEngine>
  get() = listOf(
    WebEngine(_name = "必应", host = "cn.bing.com", start = "https://cn.bing.com/search?q="),
    WebEngine(_name = "百度", host = "m.baidu.com", start = "https://m.baidu.com/s?word="),
    WebEngine(_name = "百度", host = "www.baidu.com", start = "https://www.baidu.com/s?wd="),
    WebEngine(_name = "谷歌", host = "www.google.com", start = "https://www.google.com/search?q="),
    WebEngine(
      _name = "搜狗",
      host = "wap.sogou.com",
      start = "https://wap.sogou.com/web/searchList.jsp?keyword="
    ),
    WebEngine(_name = "搜狗", host = "www.sogou.com", start = "https://www.sogou.com/web?query="),
    WebEngine(_name = "360搜索", host = "m.so.com", start = "https://m.so.com/s?q="),
    WebEngine(_name = "360搜索", host = "www.so.com", start = "https://www.so.com/s?q="),
    WebEngine(
      _name = "雅虎", host = "search.yahoo.com", start = "https://search.yahoo.com/search?p="
    )
  )