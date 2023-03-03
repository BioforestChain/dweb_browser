package info.bagen.rust.plaoc.microService.user

import info.bagen.rust.plaoc.microService.sys.jmm.JmmMetadata
import info.bagen.rust.plaoc.microService.sys.jmm.JsMicroModule

class DesktopJMM : JsMicroModule(
    JmmMetadata(
        id = "desktop.user.dweb",
        mainUrl = "file:///bundle/desktop.worker.js"
    )
) {}