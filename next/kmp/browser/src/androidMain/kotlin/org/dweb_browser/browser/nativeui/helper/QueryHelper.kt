package org.dweb_browser.browser.nativeui.helper

import kotlinx.serialization.json.Json
import org.dweb_browser.helper.compose.ColorJson
import org.dweb_browser.microservice.http.PureRequest

class QueryHelper {
  companion object {

    fun init() {
      // 确保 init 里头的类被注册
    }
    fun color(req: PureRequest) = req.queryOrNull("color")?.let {
      Json.decodeFromString<ColorJson>(it).toColor()
    }

    fun style(req: PureRequest) = req.queryOrNull("style")?.let {
      org.dweb_browser.browser.nativeui.helper.BarStyle.from(it)
    }

    fun visible(req: PureRequest) = req.queryOrNull("visible")?.toBoolean()
    fun overlay(req: PureRequest) = req.queryOrNull("overlay")?.toBoolean()
  }
}



