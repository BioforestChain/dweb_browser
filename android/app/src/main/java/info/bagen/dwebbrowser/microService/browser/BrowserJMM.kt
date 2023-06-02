package info.bagen.dwebbrowser.microService.browser

import info.bagen.dwebbrowser.microService.browser.jmm.JmmMetadata
import info.bagen.dwebbrowser.microService.browser.jmm.JsMicroModule

class BrowserJMM : JsMicroModule(
    JmmMetadata(
        id = "browser.dweb",
        server = JmmMetadata.MainServer(
            root = "/sys",
            entry = "public.service.worker.js"
        ),
    )
) {}