package org.dweb_browser.browser.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.BitmapPainter
import dweb_browser_kmp.browser.generated.resources.Res
import dweb_browser_kmp.browser.generated.resources.ic_engine_360
import dweb_browser_kmp.browser.generated.resources.ic_engine_baidu
import dweb_browser_kmp.browser.generated.resources.ic_engine_bing
import dweb_browser_kmp.browser.generated.resources.ic_engine_duckgo
import dweb_browser_kmp.browser.generated.resources.ic_engine_google
import dweb_browser_kmp.browser.generated.resources.ic_engine_sm
import dweb_browser_kmp.browser.generated.resources.ic_engine_sogou
import dweb_browser_kmp.browser.generated.resources.ic_engine_yahoo
import io.ktor.http.Url
import kotlinx.serialization.Serializable
import org.dweb_browser.browser.BrowserDrawResource
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.createStore
import org.dweb_browser.helper.format
import org.dweb_browser.helper.platform.toImageBitmap
import org.dweb_browser.helper.toWebUrl
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Serializable
enum class SearchEngine(
  val host: String, // 域名 如：baidu.com, cn.bing.com, www.google.com,
  val keys: String, // 名称，多个可以使用“逗号”分隔，如 "baidu,百度", "google,谷歌"
  val displayName: String,
  val searchLinks: List<String>,
  val homeLink: String,
  private val iconRes: DrawableResource,
) {
  Bing(
    host = "bing.com",
    keys = "bing,必应,bing.com,www.bing.com,cn.bing.com",
    displayName = "必应",
    searchLinks = listOf(
      "http://cn.bing.com/search?q=%s", "https://www.bing.com/search?q=%s"
    ),
    homeLink = "https://www.bing.com",
    iconRes = Res.drawable.ic_engine_bing
  ),
  BaiDu(
    host = "baidu.com",
    keys = "baidu,百度,baidu.com,www.baidu.com,m.baidu.com",
    displayName = "百度",
    searchLinks = listOf("https://m.baidu.com/s?word=%s", "https://www.baidu.com/s?wd=%s"),
    homeLink = "https://www.baidu.com",
    iconRes = Res.drawable.ic_engine_baidu
  ),
  Google(
    host = "google.com",
    keys = "Google,谷歌,google.com,www.google.com",
    displayName = "Google",
    searchLinks = listOf("https://www.google.com/search?q=%s"),
    homeLink = "https://www.google.com",
    iconRes = Res.drawable.ic_engine_google
  ),
  So360(
    host = "so.com",
    keys = "360,so.com,www.so.com,m.so.com",
    displayName = "360",
    searchLinks = listOf("https://m.so.com/s?q=%s", "https://www.so.com/s?q=%s"),
    homeLink = "https://www.so.com/",
    iconRes = Res.drawable.ic_engine_360
  ),
  SoGou(
    host = "sogou.com",
    keys = "sogou,搜狗,sogou.com,www.sogou.com,wap.sogou.com",
    displayName = "搜狗",
    searchLinks = listOf(
      "https://wap.sogou.com/web/searchList.jsp?keyword=%s", "https://www.sogou.com/web?query=%s"
    ),
    homeLink = "https://www.sogou.com",
    iconRes = Res.drawable.ic_engine_sogou
  ),
  DuckDuckGo(
    host = "duckduckgo.com",
    keys = "DuckDuckGo,duckduckgo.com",
    displayName = "DuckDuckGo",
    searchLinks = listOf("https://duckduckgo.com/?q=%s"),
    homeLink = "https://duckduckgo.com",
    iconRes = Res.drawable.ic_engine_duckgo
  ),
  Yahoo(
    host = "yahoo.com",
    keys = "yahoo,雅虎,yahoo.com,sg.search.yahoo.com,search.yahoo.com",
    displayName = "雅虎",
    searchLinks = listOf(
      "https://search.yahoo.com/search?p=%s", "https://sg.search.yahoo.com/search?p=%s"
    ),
    homeLink = "https://sg.search.yahoo.com/",
    iconRes = Res.drawable.ic_engine_yahoo
  ),
  SM(
    host = "m.sm.cn",
    keys = "神马,sm.cn,so.m.sm.cn,m.sm.cn",
    displayName = "神马",
    searchLinks = listOf("https://so.m.sm.cn/s?q=%s", "https://m.sm.cn/s?q=%s"),
    homeLink = "https://so.m.sm.cn",
    iconRes = Res.drawable.ic_engine_sm
  ),
  ;

  var enable by mutableStateOf(false)

  @Composable
  fun painter() = painterResource(iconRes)

  fun matchKeyWord(keyWord: String): Boolean {
    val searchUrls = searchLinks.map { link -> Url(link.format("test")) }
    return keyWord.toWebUrl()?.let { keyWordUrl ->
      val searchUrl1 = searchUrls.firstOrNull { it.host == keyWordUrl.host }
      if (searchUrl1 != null) {
        searchUrls.firstOrNull { searchUrl ->
          keyWordUrl.parameters[searchUrl.parameters.names().first()] != null
        } != null
      } else false
    } ?: run {
      keys.split(",").find { it == keyWord } != null
    }
  }

  /**
   * 获取关键字对应的内容，用于搜索栏显示
   */
  fun queryKeyWordValue(url: Url): String? {
    val searchUrls = searchLinks.map { link -> Url(link.format("test")) }
    return searchUrls.firstOrNull { url.host == it.host }?.let { findUrl ->
      val keyWord = findUrl.parameters.names().first()
      url.parameters[keyWord]
    }
  }
}

//@OptIn(ExperimentalResourceApi::class)
//@Serializable
//data class SearchEngine(
//  val host: String, // 域名 如：baidu.com, cn.bing.com, www.google.com,
//  val keys: String, // 名称，多个可以使用“逗号”分隔，如 "baidu,百度", "google,谷歌"
//  val name: String,
//  val searchLinks: List<String>,
//  val homeLink: String,
//  val iconRes: DrawableResource,
//  var enable: Boolean = false
//) {
//  fun matchKeyWord(keyWord: String): Boolean {
//    val searchUrls = searchLinks.map { link -> Url(link.format("test")) }
//    return keyWord.toWebUrl()?.let { keyWordUrl ->
//      val searchUrl1 = searchUrls.firstOrNull { it.host == keyWordUrl.host }
//      if (searchUrl1 != null) {
//        searchUrls.firstOrNull { searchUrl ->
//          keyWordUrl.parameters[searchUrl.parameters.names().first()] != null
//        } != null
//      } else false
//    } ?: run {
//      keys.split(",").find { it == keyWord } != null
//    }
//  }
//
//  /**
//   * 获取关键字对应的内容，用于搜索栏显示
//   */
//  fun queryKeyWordValue(url: Url): String? {
//    val searchUrls = searchLinks.map { link -> Url(link.format("test")) }
//    return searchUrls.firstOrNull { url.host == it.host }?.let { findUrl ->
//      val keyWord = findUrl.parameters.names().first()
//      url.parameters[keyWord]
//    }
//  }
//
//
//}
//
//@OptIn(ExperimentalResourceApi::class)
//val SearchEngineList = mutableStateListOf(
//  SearchEngine(
//    host = "baidu.com",
//    keys = "baidu,百度,www.baidu.com,m.baidu.com",
//    name = "百度",
//    searchLinks = listOf(
//      "https://m.baidu.com/s?word=%s", "https://www.baidu.com/s?wd=%s"
//    ),
//    homeLink = "https://www.baidu.com",
//    iconRes = Res.drawable.ic_engine_baidu,
//  ),
//  SearchEngine(
//    host = "bing.com",
//    keys = "bing,必应,www.bing.com,cn.bing.com",
//    name = "必应",
//    searchLinks = listOf(
//      "http://cn.bing.com/search?q=%s", "https://www.bing.com/search?q=%s"
//    ),
//    homeLink = "https://www.bing.com",
//    iconRes = Res.drawable.ic_engine_bing,
//  ),
//  SearchEngine(
//    host = "sogou.com",
//    keys = "sogou,搜狗,www.sogou.com,wap.sogou.com",
//    name = "搜狗",
//    searchLinks = listOf(
//      "https://wap.sogou.com/web/searchList.jsp?keyword=%s", "https://www.sogou.com/web?query=%s"
//    ),
//    homeLink = "https://www.sogou.com",
//    iconRes = Res.drawable.ic_engine_sogou,
//  ),
//  SearchEngine(
//    host = "so.com",
//    keys = "360,www.so.com,m.so.com",
//    name = "360",
//    searchLinks = listOf("https://m.so.com/s?q=%s", "https://www.so.com/s?q=%s"),
//    homeLink = "https://www.so.com/",
//    iconRes = Res.drawable.ic_engine_360,
//  ),
//  SearchEngine(
//    host = "google.com",
//    keys = "Google,谷歌,www.google.com",
//    name = "Google",
//    searchLinks = listOf("https://www.google.com/search?q=%s"),
//    homeLink = "https://www.google.com",
//    iconRes = Res.drawable.ic_engine_google,
//  ),
//  SearchEngine(
//    host = "duckduckgo.com",
//    keys = "DuckDuckGo,duckduckgo.com",
//    name = "DuckDuckGo",
//    searchLinks = listOf("https://duckduckgo.com/?q=%s"),
//    homeLink = "https://duckduckgo.com",
//    iconRes = Res.drawable.ic_engine_duckgo,
//  ),
//  SearchEngine(
//    host = "yahoo.com",
//    keys = "yahoo,雅虎,sg.search.yahoo.com,search.yahoo.com",
//    name = "雅虎",
//    searchLinks = listOf(
//      "https://search.yahoo.com/search?p=%s", "https://sg.search.yahoo.com/search?p=%s"
//    ),
//    homeLink = "https://sg.search.yahoo.com/",
//    iconRes = Res.drawable.ic_engine_yahoo,
//  ),
//  SearchEngine(
//    host = "m.sm.cn",
//    keys = "神马,so.m.sm.cn,m.sm.cn",
//    name = "神马",
//    searchLinks = listOf("https://so.m.sm.cn/s?q=%s", "https://m.sm.cn/s?q=%s"),
//    homeLink = "https://so.m.sm.cn",
//    iconRes = Res.drawable.ic_engine_sm,
//  ),
//)

@Serializable
data class SearchInject(
  val name: String = "unKnow", // 表示应用名称
  val icon: ByteArray? = null, // 表示应用的图标
  val url: String, // ipc 链接、https 链接
) {
  @Composable
  fun iconPainter() = key(icon) { icon?.toImageBitmap()?.let { BitmapPainter(it) } }
    ?: BrowserDrawResource.Web.painter()
}

class SearchStore(mm: MicroModule.Runtime) {
  private val keyInject = "key_inject"
  private val storeEngine = mm.createStore("engines_state", false)
  private val storeInject = mm.createStore("inject_engine", false)

  suspend fun getAllEnginesState(): List<SearchEngine> {
    val save = storeEngine.getAll<Boolean>()
    return SearchEngine.entries.onEach { item ->
      item.enable = save[item.host] ?: false
    }
  }

  suspend fun saveEngineState(searchEngine: SearchEngine) {
    storeEngine.set(searchEngine.host, searchEngine.enable)
  }

  suspend fun getAllInjects(): MutableList<SearchInject> {
    return storeInject.getOrPut<MutableList<SearchInject>>(keyInject) {
      mutableStateListOf()
    }
  }

  suspend fun saveInject(list: MutableList<SearchInject>) {
    storeInject.set(keyInject, list)
  }
}