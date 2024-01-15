package org.dweb_browser.browser.web.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.ktor.http.Url
import io.ktor.http.fullPath
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.dweb_browser.browser.BrowserIconResource
import org.dweb_browser.browser.getIconResource
import org.dweb_browser.browser.web.data.WebSiteInfo
import org.dweb_browser.helper.compose.ObservableMutableState
import org.dweb_browser.helper.compose.compositionChainOf
import org.dweb_browser.helper.platform.toByteArray
import org.dweb_browser.helper.platform.toImageBitmap

val LocalModalBottomSheet = compositionChainOf<ModalBottomModel>("LocalModalBottomSheet")

enum class SheetState {
  Hidden, Expanded, PartiallyExpanded;

  fun defaultHeight(maxHeight: Float) = when (this) {
    Hidden -> 0f
    Expanded -> maxHeight
    else -> maxHeight * 2 / 3
  }
}

enum class PageType {
  Home, BookManager, EngineList, EngineManger;
}

enum class PopupViewState(
  private val height: Dp = 0.dp,
  private val percentage: Float? = null,
  val title: String,
  val imageVector: ImageVector,
  val index: Int,
) {
  Options(height = 120.dp, title = "选项", imageVector = Icons.Default.Menu, index = 0),
  BookList(percentage = 0.9f, title = "书签列表", imageVector = Icons.Default.Book, index = 1),
  HistoryList(
    percentage = 0.9f,
    title = "历史记录",
    imageVector = Icons.Default.History,
    index = 2
  ),
  // Share(percentage = 0.5f, title = "分享"),
  ;

  fun getLocalHeight(screenHeight: Dp? = null): Dp {
    return screenHeight?.let {
      percentage?.let { percentage ->
        screenHeight * percentage
      }
    } ?: height
  }
}

class ModalBottomModel {
  val state: MutableState<SheetState> = mutableStateOf(SheetState.PartiallyExpanded)
  val show: MutableState<Boolean> = mutableStateOf(false)
  val tabIndex = mutableStateOf(PopupViewState.Options)
  val pageType = mutableStateOf(PageType.Home)
  val webSiteInfo: MutableState<WebSiteInfo?> = mutableStateOf(null)

  suspend fun hide() {
    state.value = SheetState.Hidden
    pageType.value = PageType.Home
    delay(50)
    show.value = false
  }

  suspend fun show() {
    show.value = true
    delay(50)
    state.value = SheetState.PartiallyExpanded
  }
}

/**
 * 该文件主要定义搜索引擎和引擎默认值，以及配置存储
 */
@Serializable
data class WebEngine(
  @SerialName("name")
  private var _name: String,
  val host: String,
  var start: String,
  var timeMillis: String = "",
  private var icon: ByteArray? = null,
  @SerialName("checked")
  private var _checked: Boolean = false,
) {
  var checked by ObservableMutableState(_checked) { _checked = it }
  var name by ObservableMutableState(_name) { _name = it }

  @Transient
  var iconRes: ImageBitmap? = null
    get() = icon?.toImageBitmap() ?: getIconResource(BrowserIconResource.WebEngineDefault)

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

  override fun toString(): String {
    return "WebEngine(name=$name, host=$host, start=$start, timeMillis=$timeMillis, checked=$_checked)"
  }
}

val DefaultSearchWebEngine = listOf(
  WebEngine(
    _name = "百度",
    host = "m.baidu.com",
    icon = getIconResource(BrowserIconResource.WebEngineBaidu)?.toByteArray(),
    start = "https://m.baidu.com/s?word="
  ),
  /*WebEngine(
    _name = "必应",
    host = "cn.bing.com",
    icon = getIconResource(BrowserIconResource.WebEngineBing)?.toByteArray(),
    start = "https://cn.bing.com/search?q="
  ),*/
  WebEngine(
    _name = "搜狗",
    host = "wap.sogou.com",
    icon = getIconResource(BrowserIconResource.WebEngineSougou)?.toByteArray(),
    start = "https://wap.sogou.com/web/searchList.jsp?keyword="
  ),
  WebEngine(
    _name = "360",
    host = "m.so.com",
    icon = getIconResource(BrowserIconResource.WebEngine360)?.toByteArray(),
    start = "https://m.so.com/s?q="
  ),
)

val DefaultAllWebEngine: List<WebEngine>
  get() = listOf(
    WebEngine(_name = "必应", host = "cn.bing.com", start = "https://cn.bing.com/search?q="),
    WebEngine(_name = "百度", host = "m.baidu.com", start = "https://m.baidu.com/s?word="),
    WebEngine(_name = "百度", host = "www.baidu.com", start = "https://www.baidu.com/s?wd="),
    WebEngine(
      _name = "谷歌", host = "www.google.com", start = "https://www.google.com/search?q="
    ),
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