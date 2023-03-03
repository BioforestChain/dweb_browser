package info.bagen.rust.plaoc.microService.user

import info.bagen.rust.plaoc.microService.sys.jmm.JmmMetadata
import info.bagen.rust.plaoc.microService.sys.jmm.JsMicroModule

class CotJMM : JsMicroModule(
    JmmMetadata(
        id = "cot.bfs.dweb",
        server = JmmMetadata.MainServer(
            root = "file:///bundle",
            entry = "/cot.worker.js"
        ),
    )
) {}