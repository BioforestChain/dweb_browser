package info.bagen.dwebbrowser.microService.desktop.db

import androidx.compose.runtime.mutableStateListOf
import info.bagen.dwebbrowser.microService.browser.jmm.JsMicroModule
import info.bagen.dwebbrowser.microService.core.WindowAppInfo
import org.dweb_browser.helper.JmmAppInstallManifest
import org.dweb_browser.helper.MainServer

val desktopAppList by lazy {
  val list = mutableStateListOf<WindowAppInfo>()
  list.add(createAppInfo("http://linge.plaoc.com/douyu.png", "https://m.douyu.com/", "斗鱼"))
  list.add(createAppInfo("http://linge.plaoc.com/163.png", "https://3g.163.com/", "网易"))
  list.add(createAppInfo("http://linge.plaoc.com/weibo.png", "https://m.weibo.cn/", "微博"))
  list.add(
    createAppInfo(
      "http://linge.plaoc.com/douban.png",
      "https://m.douban.com/movie/",
      "豆瓣"
    )
  )
  list.add(createAppInfo("http://linge.plaoc.com/zhihu.png", "https://www.zhihu.com/", "知乎"))
  list.add(
    createAppInfo(
      "http://linge.plaoc.com/bilibili.png",
      "https://m.bilibili.com/",
      "哔哩哔哩"
    )
  )
  list.add(
    createAppInfo(
      "http://linge.plaoc.com/tencent.png",
      "https://xw.qq.com/?f=qqcom",
      "腾讯新闻"
    )
  )
  list
}

internal fun createAppInfo(icon: String, url: String, name: String) = WindowAppInfo(
  jsMicroModule = JsMicroModule(JmmAppInstallManifest(
    id = name,
    server = MainServer("", ""),
    icon = icon,
    bundle_url = url,
    name = name,
  ))
)
