package org.dweb_browser.browser.web.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.browser.search.SearchEngine
import org.dweb_browser.browser.search.SearchInject
import org.dweb_browser.browser.web.BrowserNMM
import org.dweb_browser.core.module.channelRequest
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.buildUrlString
import org.dweb_browser.pure.http.IPureBody
import org.dweb_browser.pure.http.PureChannelContext
import org.dweb_browser.pure.http.PureClientRequest
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.PureTextFrame

suspend fun BrowserNMM.BrowserRuntime.getEngineHomeLink(key: String) =
  nativeFetch(buildUrlString("file://search.browser.dweb/homeLink") {
    parameters["key"] = key
  }).text()

suspend fun BrowserNMM.BrowserRuntime.updateEngineState(
  searchEngine: SearchEngine, enabled: Boolean,
) = nativeFetch(
  PureClientRequest(
    href = buildUrlString("file://search.browser.dweb/updateEngineState") {
      parameters["state"] = enabled.toString()
    },
    PureMethod.POST,
    body = IPureBody.from(Json.encodeToString(searchEngine))
  )
)

suspend fun BrowserNMM.BrowserRuntime.getInjectList(key: String) =
  Json.decodeFromString<MutableList<SearchInject>>(
    nativeFetch(buildUrlString("file://search.browser.dweb/injectList") {
      parameters["key"] = key
    }).text()
  )

class WatchSearchEngineContext(val engineList: List<SearchEngine>, val channel: PureChannelContext)

suspend fun BrowserNMM.BrowserRuntime.collectChannelOfEngines(collector: suspend WatchSearchEngineContext.() -> Unit) =
  channelRequest("file://search.browser.dweb/observe/engines") {
    for (pureFrame in income) {
      when (pureFrame) {
        is PureTextFrame -> {
          WatchSearchEngineContext(
            Json.decodeFromString<List<SearchEngine>>(pureFrame.text), this
          ).collector()
        }

        else -> {}
      }
    }
  }


/**
 * 是否是链接的开头部分
 */
internal fun String.couldBeUrlStart() = isEmpty()//
    || split(":", limit = 2).let { it.size > 1 && it.first().matches(Regex("^[\\w+]+")) }//
    || "data:".startsWith(this)  //
    || "https://".startsWith(this)  //
    || "dweb://".startsWith(this)  //
    || "about://".startsWith(this)  //