package info.bagen.dwebbrowser.microService.desktop.db

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import org.dweb_browser.helper.AppMetaData
import info.bagen.dwebbrowser.microService.desktop.model.AppInfo
import org.dweb_browser.helper.MainServer

val desktopAppList by lazy {
  val list = mutableStateListOf<AppInfo>()
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

internal fun createAppInfo(icon: String, url: String, name: String) = AppInfo(
  appMetaData = AppMetaData(
    id = name,
    server = MainServer("", ""),
    icon = icon,
    bundle_url = url,
    name = name,
  ),
  zoom = mutableFloatStateOf(0.7f)
)
