package info.bagen.rust.plaoc.microService.apps

import info.bagen.rust.plaoc.microService.sys.jmm.JmmMetadata
import info.bagen.rust.plaoc.microService.sys.jmm.JsMicroModule

class BrowserJMM : JsMicroModule(
    JmmMetadata(
        id = "browser.sys.dweb",
        main_url = "file:///bundle/browser/server/index.js"
    )
) {}