package org.dweb_browser.browser.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.painter.BitmapPainter
import io.ktor.http.Url
import kotlinx.serialization.Serializable
import org.dweb_browser.browser.BrowserDrawResource
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.createStore
import org.dweb_browser.helper.format
import org.dweb_browser.helper.platform.toImageBitmap
import org.dweb_browser.helper.toWebUrlOrWithoutProtocol

@Serializable
data class SearchEngine(
  val host: String, // 域名 如：baidu.com, cn.bing.com, www.google.com,
  val keys: String, // 名称，多个可以使用“逗号”分隔，如 "baidu,百度", "google,谷歌"
  val name: String,
  val searchLink: String,
  val homeLink: String,
  val iconLink: String,
  var enable: Boolean = false
) {
  fun fit(keyWord: String): Boolean {
    val current = Url(searchLink.format("test"))
    val queryName = current.parameters.names().last()
    return keyWord.toWebUrlOrWithoutProtocol()?.let { uri ->
      uri.host == current.host && uri.parameters[queryName] != null
    } ?: run {
      keys.split(",").find { it == keyWord } != null
    }
  }

  fun queryName(): String {
    val current = Url(searchLink.format("test"))
    return current.parameters.names().last()
  }
}

val SearchEngineList = mutableStateListOf(
  SearchEngine(
    host = "baidu.com",
    keys = "baidu,百度,www.baidu.com",
    name = "百度",
    searchLink = "https://www.baidu.com/s?wd=%s",
    homeLink = "https://www.baidu.com",
    iconLink = "file:///sys/engines/baidu.svg",
  ),
  SearchEngine(
    host = "bing.com",
    keys = "bing,必应,www.bing.com",
    name = "Bing",
    searchLink = "https://www.bing.com/search?q=%s",
    homeLink = "https://www.bing.com",
    iconLink = "file:///sys/engines/bing.svg",
  ),
  SearchEngine(
    host = "sogou.com",
    keys = "sogou,搜狗,www.sogou.com",
    name = "搜狗",
    searchLink = "https://www.sogou.com/web?query=%s",
    homeLink = "https://www.sogou.com",
    iconLink = "file:///sys/engines/sogou.svg",
  ),
  SearchEngine(
    host = "so.com",
    keys = "360,www.so.com",
    name = "360",
    searchLink = "https://www.so.com/s?q=%s",
    homeLink = "https://www.so.com/",
    iconLink = "file:///sys/engines/360.svg",
  ),
  SearchEngine(
    host = "google.com",
    keys = "Google,谷歌,www.google.com",
    name = "Google",
    searchLink = "https://www.google.com/search?q=%s",
    homeLink = "https://www.google.com",
    iconLink = "file:///sys/engines/google.svg",
  ),
  SearchEngine(
    host = "duckduckgo.com",
    keys = "DuckDuckGo,duckduckgo.com",
    name = "DuckDuckGo",
    searchLink = "https://duckduckgo.com/?q=%s",
    homeLink = "https://duckduckgo.com",
    iconLink = "file:///sys/engines/duckgo.svg",
  ),
  SearchEngine(
    host = "yahoo.com",
    keys = "yahoo,雅虎,sg.search.yahoo.com,search.yahoo.com",
    name = "雅虎",
    searchLink = "https://sg.search.yahoo.com/search;?p=%s",
    homeLink = "https://sg.search.yahoo.com/",
    iconLink = "file:///sys/engines/yahoo.svg",
  ),
  SearchEngine(
    host = "m.sm.cn",
    keys = "神马,so.m.sm.cn,m.sm.cn",
    name = "神马",
    searchLink = "https://so.m.sm.cn/s?q=%s",
    homeLink = "https://so.m.sm.cn",
    iconLink = "file:///sys/engines/sm.svg",
  ),
)

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

class SearchStore(mm: MicroModule) {
  private val keyInject = "key_inject"
  private val storeEngine = mm.createStore("engines_state", false)
  private val storeInject = mm.createStore("inject_engine", false)

  suspend fun getAllEnginesState(): MutableList<SearchEngine> {
    val save = storeEngine.getAll<Boolean>()
    return SearchEngineList.onEach { item ->
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