package info.bagen.rust.plaoc.microService.user

import info.bagen.rust.plaoc.microService.core.JmmMetadata
import info.bagen.rust.plaoc.microService.core.JsMicroModule

class DesktopJMM :
    JsMicroModule("desktop.user.dweb", JmmMetadata(main_url = "file:///bundle/desktop.worker.js")) {
}