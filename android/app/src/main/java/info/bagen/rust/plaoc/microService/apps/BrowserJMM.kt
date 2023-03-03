package info.bagen.rust.plaoc.microService.apps

import info.bagen.rust.plaoc.microService.sys.jmm.JmmMetadata
import info.bagen.rust.plaoc.microService.sys.jmm.JsMicroModule

class BrowserJMM : JsMicroModule(
    JmmMetadata(
        id = "browser.sys.dweb",
        server = JmmMetadata.MainServer(
            root = "file:///bundle/browser/server",
            entry = "/index.js"
        ),
    )
) {}