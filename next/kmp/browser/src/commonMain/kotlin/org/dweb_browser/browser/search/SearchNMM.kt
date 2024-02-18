package org.dweb_browser.browser.search

import io.ktor.http.HttpStatusCode
import org.dweb_browser.browser.BrowserI18nResource
import org.dweb_browser.core.help.types.MICRO_MODULE_CATEGORY
import org.dweb_browser.core.http.router.bind
import org.dweb_browser.core.module.BootstrapContext
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.helper.Debugger
import org.dweb_browser.helper.DisplayMode
import org.dweb_browser.helper.ImageResource
import org.dweb_browser.helper.toJsonElement
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
       * 将当前搜索引擎置为有效
       */
      "enable" bind PureMethod.GET by defineBooleanResponse {
        val key = request.queryOrNull("key")
        debugSearch("browser/enable", "key=$key")
        key?.let {
          controller.enableEngine(key)
        } ?: throwException(HttpStatusCode.BadRequest, "not found key param")
      },
      /**
       * 搜索所有可用引擎
       */
      "engines" bind PureMethod.GET by defineJsonResponse {
        val key = request.queryOrNull("key")
        debugSearch("browser/engines", "key=$key")
        key?.let {
          controller.engineSearch(key)?.toJsonElement()
            ?: throwException(HttpStatusCode.NotFound, "not found engine for $key")
        } ?: throwException(HttpStatusCode.BadRequest, "not found key param")
      },
      /**
       * 搜索都有注入的搜索列表
       */
      "inject" bind PureMethod.GET by defineJsonResponse {
        val key = request.queryOrNull("key")
        debugSearch("browser/inject", "key=$key")
        throwException(HttpStatusCode.BadRequest, "not found key param")
      },
    )

    protocol("search.std.dweb") {
      routes(
        "inject" bind PureMethod.POST by defineBooleanResponse {
          debugSearch("std/inject")
          val injectEngine = request.queryAs<InjectEngine>()
          true
        }
      )
    }
  }

  override suspend fun _shutdown() {
  }
}