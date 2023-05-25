package info.bagen.dwebbrowser.microService.apps

import info.bagen.dwebbrowser.microService.sys.jmm.JmmMetadata
import info.bagen.dwebbrowser.microService.sys.jmm.JsMicroModule

class BrowserJMM : JsMicroModule(
    JmmMetadata(
        id = "browser.sys.dweb",
        server = JmmMetadata.MainServer(
            root = "/sys",
            entry = "public.service.worker.js"
        ),
    )
) {}