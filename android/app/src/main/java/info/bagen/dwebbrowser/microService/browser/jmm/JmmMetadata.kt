package info.bagen.dwebbrowser.microService.browser.jmm

import info.bagen.dwebbrowser.microService.helper.DWEB_DEEPLINK
import info.bagen.dwebbrowser.microService.helper.Mmid

data class JmmMetadata(
    val id: Mmid, // jmmApp的id
    val server: MainServer, // 打开应用地址
    val dweb_deeplinks: MutableList<DWEB_DEEPLINK> = mutableListOf(),
    val title: String = "", // 应用名称
    val subtitle: String = "", // 应用副标题
    val icon: String = "", // 应用图标
    val downloadUrl: String = "", // 下载应用地址
    val images: List<String>? = null, // 应用截图
    val introduction: String = "", // 应用描述
    val author: List<String>? = null, // 开发者，作者
    val version: String = "", // 应用版本
    val newFeature: String = "", // 新特性，新功能
    val keywords: List<String>? = null, // 关键词
    val home: String = "", // 首页地址
    val size: String = "", // 应用大小
    val fileHash: String = "", // 文件hash
    val permissions: List<String>? = null, // app使用权限的情况
    val plugins: List<String>? = null, // app使用插件的情况
    val releaseDate: String = "", // 发布时间
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

val defaultJmmMetadata =  JmmMetadata(
  id = "default.user.dweb",
  downloadUrl = "https://dweb.waterbang.top/game.dweb.waterbang.top.dweb.jmm",
  permissions = arrayListOf("camera.sys.dweb", "jmm.sys.dweb", "???.sys.dweb"),
  icon = "https://www.bfmeta.info/imgs/logo3.webp",
  title = "默认测试数据",
  subtitle = "该测试数据包含了相关具体信息，请仔细查阅",
  introduction = "这是一个实例应用，包含了dweb_plugins全部组件的实例。",
  size = "2726400",
  version = "1.2.0",
  images = listOf(
    "http://qiniu-waterbang.waterbang.top/bfm/cot-home_2058.webp",
    "http://qiniu-waterbang.waterbang.top/bfm/defi.png",
    "http://qiniu-waterbang.waterbang.top/bfm/nft.png",
    "http://qiniu-waterbang.waterbang.top/bfm/nft.png",
    "http://qiniu-waterbang.waterbang.top/bfm/nft.png"
  ),
  home = "https://www.bfmeta.info/",
  server = JmmMetadata.MainServer(root = "dweb:///sys", entry = "/bfs_worker/public.service.worker.js"),
  author = listOf("bfs", "bfs@bfs.com"),
  keywords = listOf("demo", "vue3"),
  releaseDate = "2023-05-22T09:01:38.318Z",
)
