package info.bagen.rust.plaoc.microService.user

import info.bagen.rust.plaoc.microService.sys.jmm.JmmMetadata
import info.bagen.rust.plaoc.microService.sys.jmm.JmmNMM.Companion.getAndUpdateJmmNmmApps
import info.bagen.rust.plaoc.microService.sys.jmm.JsMicroModule

class CotDemoJMM : JsMicroModule(
    JmmMetadata(
        id = "cotdemo.bfs.dweb",  // TODO warning 不能写大写
        server = JmmMetadata.MainServer(
            root = "file:///bundle",
            entry = "/cotDemo.worker.js"
        ),
        splashScreen = JmmMetadata.SplashScreen("https://www.bfmeta.org/")
    )
) {
   init {
       // TODO 测试打开的需要把metadata添加到 jmm app
       getAndUpdateJmmNmmApps()[mmid] = this
   }
}