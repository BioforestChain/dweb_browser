package info.bagen.dwebbrowser.microService.user

import info.bagen.dwebbrowser.microService.sys.jmm.JmmMetadata
import info.bagen.dwebbrowser.microService.sys.jmm.JmmNMM
import info.bagen.dwebbrowser.microService.sys.jmm.JsMicroModule

class CotJMM : JsMicroModule(
    JmmMetadata(
        id = "cot.bfs.dweb",
        server = JmmMetadata.MainServer(
            root = "/sys",
            entry = "/bundle/public.service.worker.js"
        ),
        icon = "https://www.bfmeta.info/imgs/logo3.webp",
        title = "Cot"
    )
) {
    init {
        // TODO 测试打开的需要把metadata添加到 jmm app
        JmmNMM.getAndUpdateJmmNmmApps()[mmid] = this
    }
}