package info.bagen.dwebbrowser.microService.user

import info.bagen.dwebbrowser.microService.browser.jmm.JmmMetadata
import info.bagen.dwebbrowser.microService.browser.jmm.JmmNMM
import info.bagen.dwebbrowser.microService.browser.jmm.JsMicroModule

class DesktopJMM : JsMicroModule(
    JmmMetadata(
        id = "desktop.dweb.waterbang.top.dweb",  // TODO warning 不能写大写
        version = "1.0.0",
        server = JmmMetadata.MainServer(
            root = "/sys",
            entry = "public.service.worker.js"
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