package info.bagen.dwebbrowser.microService.user

import info.bagen.dwebbrowser.microService.sys.jmm.JmmMetadata
import info.bagen.dwebbrowser.microService.sys.jmm.JsMicroModule

class DesktopJMM : JsMicroModule(
    JmmMetadata(
        id = "desktop.user.dweb",
        server = JmmMetadata.MainServer(
            root = "file:///bundle",
            entry = "/desktop.worker.js"
        ),
    )
) {}