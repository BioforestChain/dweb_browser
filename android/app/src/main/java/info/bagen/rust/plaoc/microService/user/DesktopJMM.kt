package info.bagen.rust.plaoc.microService.user

import info.bagen.rust.plaoc.microService.sys.jmm.JmmMetadata
import info.bagen.rust.plaoc.microService.sys.jmm.JsMicroModule

class DesktopJMM : JsMicroModule(
    "desktop.user.dweb",
    JmmMetadata(main_url = "file:///bundle/desktop.worker.js")
) {}