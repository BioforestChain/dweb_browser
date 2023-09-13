package info.bagen.dwebbrowser.microService.browser.nativeui.helper

import kotlinx.serialization.json.Json
import org.dweb_browser.helper.compose.ColorJson
import org.dweb_browser.microservice.http.PureRequest

class QueryHelper {
  companion object {

    fun init() {
      // 确保 init 里头的类被注册
    }
    fun color(req: PureRequest) = req.query("color")?.let {
      Json.decodeFromString<ColorJson>(it).toColor()
    }

    fun style(req: PureRequest) = req.query("style")?.let {
      BarStyle.from(it)
    }

    fun visible(req: PureRequest) = req.query("visible")?.toBoolean()
    fun overlay(req: PureRequest) = req.query("overlay")?.toBoolean()
  }
}



