package info.bagen.dwebbrowser.microService.browser.jmm

import org.dweb_browser.microservice.help.DWEB_DEEPLINK
import org.dweb_browser.microservice.help.Mmid

data class JmmMetadata(
  val id: Mmid, // jmmApp的id
  val server: MainServer, // 打开应用地址
  val dweb_deeplinks: MutableList<DWEB_DEEPLINK> = mutableListOf(),
  val name: String = "", // 应用名称
  val short_name: String = "", // 应用副标题
  val icon: String = "", // 应用图标
  val images: List<String>? = null, // 应用截图
  val description: String = "", // 应用描述
  val author: List<String>? = null, // 开发者，作者
  val categories: List<String>? = null, // 应用类型 https://github.com/w3c/manifest/wiki/Categories
  val version: String = "", // 应用版本
  val new_feature: String? = null, // 新特性，新功能
  val home: String = "", // 首页地址
  var bundle_url: String = "", // 下载应用地址
  val bundle_size: String = "", // 应用大小
  val bundle_hash: String = "", // 文件hash
  val permissions: List<String>? = null, // app使用权限的情况
  val plugins: List<String>? = null, // app使用插件的情况
  val release_date: String = "", // 发布时间
) : java.io.Serializable {
    data class MainServer(
        /**
         * 应用文件夹的目录
         */
        val root: String,
        /**
         * 入口文件
         */
        val entry: String
    ) : java.io.Serializable


}


val defaultJmmMetadata = JmmMetadata(
    id = "demo.user.dweb",
    dweb_deeplinks = mutableListOf(),
    icon = "https://www.bfmeta.info/imgs/logo3.webp",
    name = "plaoc demo",
    short_name = "demo",
    description = "这是一个实例应用，包含了dweb_plugins全部组件的实例。",
    bundle_url = "https://dweb.waterbang.top/game.dweb.waterbang.top.dweb-1.0.0.zip",
    bundle_size = "2742079",
    bundle_hash = "sha256:966251757d12a7f4021a0b40031ff4b2b60ae08b63de516b572eb572bfd90886", // 文件hash
    version = "1.2.0",
    images = listOf(
        "http://qiniu-waterbang.waterbang.top/bfm/cot-home_2058.webp",
        "http://qiniu-waterbang.waterbang.top/bfm/defi.png",
        "http://qiniu-waterbang.waterbang.top/bfm/nft.png"
    ),
    home = "https://dweb-browser.org/",
    server = JmmMetadata.MainServer(
        root = "/sys",
        entry = "/server/plaoc.server.js"
    ),
    author = listOf("bfs", "bfs@bfs.com"),
    categories = listOf("demo", "vue3","plaoc"),
    release_date = "2023-05-22T09:01:38.318Z",
)
