package info.bagen.dwebbrowser.microService.user

import info.bagen.dwebbrowser.microService.sys.jmm.JmmMetadata
import info.bagen.dwebbrowser.microService.sys.jmm.JsMicroModule

class ToyJMM : JsMicroModule(
    JmmMetadata(
        id = "toy.bfs.dweb",
        server = JmmMetadata.MainServer(
            root = "file:///bundle", entry = "/toy.worker.js"
        ),
    )
) {}