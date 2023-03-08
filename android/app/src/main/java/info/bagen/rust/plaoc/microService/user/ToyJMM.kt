package info.bagen.rust.plaoc.microService.user

import info.bagen.rust.plaoc.microService.sys.jmm.JmmMetadata
import info.bagen.rust.plaoc.microService.sys.jmm.JsMicroModule

class ToyJMM : JsMicroModule(
    JmmMetadata(
        id = "toy.bfs.dweb",
        server = JmmMetadata.MainServer(
            root = "file:///bundle", entry = "/toy.worker.js"
        ),
    )
) {}