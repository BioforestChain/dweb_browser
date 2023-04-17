package info.bagen.dwebbrowser.microService.sys.jmm

import info.bagen.dwebbrowser.microService.helper.Mmid

data class JmmMetadata(
  val id: Mmid,
  val server: MainServer, // 打开应用地址
  val title: String = "", // 应用名称
  val subtitle: String = "", // 应用副标题
  val icon: String = "", // 应用图标
  val downloadUrl: String = "", // 下载应用地址
  val images: List<String>? = null, // 应用截图
  val introduction: String = "", // 应用描述
  val splashScreen: SplashScreen = SplashScreen(),
  val author: List<String>? = null, // 开发者，作者
  val version: String = "", // 应用版本
  val keywords: List<String>? = null, // 关键词
  val home: String = "", // 首页地址
  val size: String = "", // 应用大小
  val fileHash: String = "",
  val permissions: List<String>? = null,
  val plugins: List<String>? = null,
  val releaseDate: String = "", // 发布时间
  /**
     * 静态网络服务
     */
    val staticWebServers: List<StaticWebServer> = listOf(),
  /**
     * 应用启动时会打开的网页
     * 要求 http/https 协议。
     * 它们会依此打开，越往后的层级越高
     *
     * TODO httpNMM 网关那边，遇到未知的请求，会等待一段时间，如果这段时间内这个域名被监听了，那么会将请求分发过去
     * 所以如果是 staticWebServers 定义的链接，那么自然而然地，页面会等到 staticWebServer 启动后得到响应，不会错过请求。
     */
    val openWebViewList: List<OpenWebView> = listOf(),
):java.io.Serializable {
    data class MainServer(
        /**
         * 应用文件夹的目录
         */
        val root: String,
        /**
         * 入口文件
         */
        val entry: String
    ):java.io.Serializable

    /**
     * 静态网络服务定义
     * 它将按配置托管一个静态网页服务
     */
    data class StaticWebServer(
        /**
         * 应用文件夹的目录
         */
        val root: String,
        /**
         * 入口文件
         */
        val entry: String = "index.html",
        val subdomain: String = "cotdemo.bfs.dweb",
        val port: Int = 80,
    ):java.io.Serializable

    data class OpenWebView(val url: String = ""):java.io.Serializable
    data class SplashScreen(val entry:String? = null):java.io.Serializable

}

val temp = """
    哔哩哔哩旗下产品，全部内容均可免费观看，边看视频边赚零花钱，邀请好友一起看更有大额现金奖励哦~
    -功能简单不占内存，旧手机也能流畅播放
    -边看热剧边赚金币，极速提现秒到账，看的越多赚的越多
    -全部内容免费观看，支持看广告解锁热门VIP内容
    -极速版专属超低价基础会员，全部内容都能直接看，新人每天低至0.17元！
    【热播剧集】
    《骑着鱼的猫》野生少女情定怨种总裁
    《微笑妈妈》王雅捷宋佳伦温情催泪
    《烽火硝烟里的青春》用血肉筑成新长城
    《今日宜加油》郑凯陈钰琪王鹤棣爆笑职场
    【超燃电影】
    《二龙湖往事：黄金劫》二龙湖浩哥再战江湖
    《黄河巨蛇事件》寻棺斩蛇破生死劫
    《龙隐迷窟》狄仁杰探墓战阴兵
    《想见你》许光汉跨越时空相恋
    【新鲜综艺】
    《一年一度喜剧大赛第2季》全新喜剧小队集结
    《我们的客栈》大型时空体验综艺
    《我们民谣2022》李宇春陪你看live
    《今晚开放麦》名人快乐分享脱口秀
    【动漫新番】
    《航海王》草帽路飞的伟大冒险
    《间谍过家家》非凡一家的绝密生活
    《名侦探柯南》小小侦探再破悬案
    《小猪佩奇》佩奇一家的欢乐日常
    《汪汪队立大功全集》超级侦探认真办案

    【金币商城 兑换会员】极速版专属会员，看剧不停兑换不止！
    【邀请好友领红包 边看视频边赚金币】看视频还能赚金币！分享给亲朋好友马上领红包，大家一起赚！还有更多福利等你来拿！
    【视频个性化推荐 海量内容随心看】新热视频应有尽有，内容推荐我懂你！海量视频片库为君筛选，看视频有我就够了~
    【极速体验 安装快开启快不占内存】手机总是提示内存不足？极速版APP安装包小不占内存，运行流畅~

    【温馨提示】有任何反馈和问题，欢迎加入哔哩哔哩极速版官方QQ群：856536591
    哔哩哔哩极速版2023更新内容
    1.「狂飙」2023年必看爆款热剧
    2. 年代剧「我们的日子」暖心热播中
    3. 今日宜加油、浮图缘、卿卿日常、人世间、叛逆者、想见你、种地吧热播中
""".trimIndent()

val defaultJmmMetadata = JmmMetadata(
    id = "cotdemo.user.dweb",
    downloadUrl = "https://shop.plaoc.com/KEJPMHLA/KEJPMHLA.bfsa",
    permissions = arrayListOf("camera.sys.dweb", "jmm.sys.dweb", "???.sys.dweb"),
    icon = "https://www.bfmeta.info/imgs/logo3.webp",
    title = "bfmeta",
    subtitle = "bfmeta",
    introduction = temp,
    size = "337408",
    version = "1.0.1.1",
    images = listOf(
        "http://qiniu-waterbang.waterbang.top/bfm/cot-home_2058.webp",
        "http://qiniu-waterbang.waterbang.top/bfm/defi.png",
        "http://qiniu-waterbang.waterbang.top/bfm/nft.png"
    ),
    home = "https://www.bfmeta.info/",
    server = JmmMetadata.MainServer(root = "file:///bundle", entry = "/cot.worker.js"),
    splashScreen = JmmMetadata.SplashScreen(null)
)

