package info.bagen.dwebbrowser.microService.user

import info.bagen.dwebbrowser.microService.sys.jmm.JmmMetadata
import info.bagen.dwebbrowser.microService.sys.jmm.JmmNMM.Companion.getAndUpdateJmmNmmApps
import info.bagen.dwebbrowser.microService.sys.jmm.JsMicroModule

class CotDemoJMM : JsMicroModule(
    JmmMetadata(
        id = "cotdemo.bfs.dweb",  // TODO warning 不能写大写
        version = "1.0.0",
        server = JmmMetadata.MainServer(
            root = "file:///bundle",
            entry = "/public.service.worker.js"
        ),
        splashScreen = JmmMetadata.SplashScreen("https://www.bfmeta.org/"),
        staticWebServers = listOf(
            JmmMetadata.StaticWebServer(
                root = "file:///bundle",
                entry = "/public.service.worker.js",
                port = 80,
                subdomain = "cotdemo.bfs.dweb"
            )
        ),
        icon = "https://www.bfmeta.info/imgs/logo3.webp",
        title = "CotDemo"
    )
) {
    init {
        // TODO 测试打开的需要把metadata添加到 jmm app
        getAndUpdateJmmNmmApps()[mmid] = this
    }
}