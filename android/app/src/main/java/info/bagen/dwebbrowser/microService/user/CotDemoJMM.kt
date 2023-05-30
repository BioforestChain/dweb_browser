package info.bagen.dwebbrowser.microService.user

import android.util.Log
import info.bagen.dwebbrowser.microService.sys.jmm.JmmMetadata
import info.bagen.dwebbrowser.microService.sys.jmm.JmmNMM.Companion.getAndUpdateJmmNmmApps
import info.bagen.dwebbrowser.microService.sys.jmm.JsMicroModule
import info.bagen.dwebbrowser.microService.sys.jmm.getOrPutOrReplace

class CotDemoJMM : JsMicroModule(
    JmmMetadata(
        id = "demo.www.bfmeta.info.dweb",  // TODO warning 不能写大写
        version = "1.0.0",
        server = JmmMetadata.MainServer(
            root = "/jmm",
            entry = "public.service.worker.js"
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