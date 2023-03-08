package info.bagen.rust.plaoc.microService.user

import info.bagen.rust.plaoc.microService.sys.jmm.JmmMetadata
import info.bagen.rust.plaoc.microService.sys.jmm.JsMicroModule

class CotDemoJMM : JsMicroModule(
    JmmMetadata(
        id = "cotdemo.bfs.dweb",  // TODO warning 不能写大写
        server = JmmMetadata.MainServer(
            root = "file:///bundle",
            entry = "/cotDemo.worker.js"
        ),
    )
) {}