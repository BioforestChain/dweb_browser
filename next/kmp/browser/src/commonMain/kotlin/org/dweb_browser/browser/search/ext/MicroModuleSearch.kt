package org.dweb_browser.browser.search.ext

import kotlinx.serialization.json.Json
import org.dweb_browser.browser.search.SearchEngine
import org.dweb_browser.browser.search.SearchInject
import org.dweb_browser.core.module.MicroModule
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.core.module.createChannel
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.pure.http.PureChannelContext
import org.dweb_browser.pure.http.PureTextFrame

suspend fun MicroModule.getEngineHomeLink(key: String) =
  nativeFetch("file://search.browser.dweb/homeLink?key=$key").text()

suspend fun MicroModule.getInjectList(key: String) =
  Json.decodeFromString<MutableList<SearchInject>>(
    nativeFetch("file://search.browser.dweb/injectList?key=$key").text()
  )

class WatchSearchEngineContext(val engineList: List<SearchEngine>, val channel: PureChannelContext)

suspend fun NativeMicroModule.collectChannelOfEngines(collector: suspend WatchSearchEngineContext.() -> Unit) =
  createChannel("file://search.browser.dweb/observe/engines") {
    for (pureFrame in income) {
      when (pureFrame) {
        is PureTextFrame -> {
          WatchSearchEngineContext(
            Json.decodeFromString<List<SearchEngine>>(pureFrame.data),
            this
          ).collector()
        }

        else -> {}
      }
    }
  }
