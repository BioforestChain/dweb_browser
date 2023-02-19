package info.bagen.rust.plaoc.microService.user

import info.bagen.rust.plaoc.microService.JmmMetadata
import info.bagen.rust.plaoc.microService.JsMicroModule

class DesktopJMM :
    JsMicroModule("desktop.user.dweb", JmmMetadata(main_url = "file:///bundle/desktop.worker.js")) {
}