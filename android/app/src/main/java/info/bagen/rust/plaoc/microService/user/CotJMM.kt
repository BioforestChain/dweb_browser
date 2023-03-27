package info.bagen.rust.plaoc.microService.user

import info.bagen.rust.plaoc.microService.sys.jmm.JmmMetadata
import info.bagen.rust.plaoc.microService.sys.jmm.JmmNMM
import info.bagen.rust.plaoc.microService.sys.jmm.JsMicroModule

class CotJMM : JsMicroModule(
    JmmMetadata(
        id = "cot.bfs.dweb",
        server = JmmMetadata.MainServer(
            root = "file:///bundle",
            entry = "/cot.worker.js"
        ),
        splashScreen = JmmMetadata.SplashScreen("https://www.bfmeta.org/")
    )
) {
    init {
        // TODO 测试打开的需要把metadata添加到 jmm app
        JmmNMM.getAndUpdateJmmNmmApps()[mmid] = this
    }
}