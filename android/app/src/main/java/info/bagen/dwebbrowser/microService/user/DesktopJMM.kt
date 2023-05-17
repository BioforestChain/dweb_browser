package info.bagen.dwebbrowser.microService.user

import info.bagen.dwebbrowser.microService.sys.jmm.JmmMetadata
import info.bagen.dwebbrowser.microService.sys.jmm.JmmNMM
import info.bagen.dwebbrowser.microService.sys.jmm.JsMicroModule

class DesktopJMM : JsMicroModule(
    JmmMetadata(
        id = "desktop.user.dweb",  // TODO warning 不能写大写
        version = "1.0.0",
        server = JmmMetadata.MainServer(
            root = "file:///bundle",
            entry = "/public.service.worker.js"
        ),
        splashScreen = JmmMetadata.SplashScreen("https://dweb.waterbang.top/"),
        staticWebServers = listOf(
            JmmMetadata.StaticWebServer(
                root = "file:///bundle",
                entry = "/public.service.worker.js",
                port = 80,
                subdomain = "desktop.user.dweb"
            )
        ),
        icon = "https://www.bfmeta.info/imgs/logo3.webp",
        title = "desktop"
    )
) {
    init {
        // TODO 测试打开的需要把metadata添加到 jmm app
        JmmNMM.getAndUpdateJmmNmmApps()[mmid] = this
    }
}