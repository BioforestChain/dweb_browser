package org.dweb_browser.browser.search

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.http.router.byChannel
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.DisplayMode
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.pure.http.PureMethod
import org.dweb_browser.pure.http.queryAs

val debugSearch = Debugger("search")

class SearchNMM : NativeMicroModule("search.browser.dweb", "Search Browser") {
  init {
    short_name = BrowserI18nResource.search_short_name.text
    categories = listOf(MICRO_MODULE_CATEGORY.Application, MICRO_MODULE_CATEGORY.Web_Browser)
    icons = listOf(ImageResource(src = "file:///sys/icons/$mmid.svg", type = "image/svg+xml"))
    display = DisplayMode.Fullscreen
  }

  override suspend fun _bootstrap(bootstrapContext: BootstrapContext) {
    val controller = SearchController(this)

    routes(
      /**
       * 判断当前是否属于引擎关键字,如果是，返回首页地址
       */
      "/check" bind PureMethod.GET by defineStringResponse {
        val key = request.queryOrNull("key")
        debugSearch("browser/enable", "key=$key")
        key?.let { controller.checkAndEnableEngine(key) ?: "" } ?: ""
      },
      /**
       * 搜索所有可用引擎
       */
      "/observe/engines" byChannel { ctx ->
        controller.onEngineUpdate {
          debugSearch("browser", "/observe/engines => send")
          ctx.sendJsonLine(controller.searchEngineList)
        }.removeWhen(onClose)
        controller.engineUpdateSignal.emit()
      },
      /**
       * 搜索都有注入的搜索列表
       */
      "/injects" bind PureMethod.GET by defineStringResponse {
        val key = request.queryOrNull("key")
        debugSearch("browser", "/injects key=$key")
        key?.let {
          val list = controller.containsInject(key)
          if (list.isNotEmpty()) {
            Json.encodeToString(list)
          } else {
            throwException(HttpStatusCode.BadRequest, "not found key param")
          }
        } ?: throwException(HttpStatusCode.BadRequest, "not found key param")
      },
    )

    protocol("search.std.dweb") {
      routes(
        /**
         * 注入离线搜索的内容
         */
        "/inject" bind PureMethod.POST by defineBooleanResponse {
          debugSearch("std/inject")
          val searchInject = request.queryAs<SearchInject>()
          debugSearch("std/inject", "injectSearch=$searchInject")
          controller.inject(searchInject)
        }
      )
    }
  }

  override suspend fun _shutdown() {
  }
}