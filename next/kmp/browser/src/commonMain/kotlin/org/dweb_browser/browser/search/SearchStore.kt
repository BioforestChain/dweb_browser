package org.dweb_browser.browser.search

import androidx.compose.runtime.mutableStateListOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.std.file.ext.createStore

@Serializable
data class SearchEngine(
  val host: String, // 域名 如：baidu.com, cn.bing.com, www.google.com,
  val name: String, // 名称，多个可以使用“逗号”分隔，如 "baidu,百度", "google,谷歌"
  val searchLink: String,
  val icon: String,
) {
  @Transient
  var enable: Boolean = false
}

val SearchEngineList = mutableStateListOf(
  SearchEngine("baidu.com", "baidu,百度", "https://www.baidu.com/s?wd=", ""),
  SearchEngine("bing.com", "bing,必应", "https://www.bing.com/search?q=", ""),
  SearchEngine("sogou.com", "sogou,搜狗", "https://www.sogou.com/web?query=", ""),
  SearchEngine("so.com", "360", "https://www.baidu.com/s?wd=", ""),
  SearchEngine("google.com", "Google,谷歌", "https://www.google.com/search?q=", ""),
  SearchEngine("duckduckgo.com", "DuckDuckGo", "https://duckduckgo.com/?q=", ""),
  SearchEngine("yahoo.com", "yahoo,雅虎", "https://sg.search.yahoo.com/search;?p=", ""),
  SearchEngine("m.sm.cn", "神马", "https://so.m.sm.cn/s?q=", ""), // "https://quark.sm.cn/s?q="
)

@Serializable
data class InjectEngine(
  val name: String = "unKnow", // 表示应用名称
  val icon: String = "", // 表示应用的图标
  val url: String, // ipc 链接、https 链接
)

class SearchStore(mm: MicroModule) {
  private val storeEngine = mm.createStore("engines_state", false)
  private val storeInject = mm.createStore("inject_engine", false)

  suspend fun getALlEnginesState(): Map<String, Boolean> {
    return storeEngine.getAll()
  }
}