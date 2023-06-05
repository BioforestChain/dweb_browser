package info.bagen.dwebbrowser.microService.user

import android.util.Log
import info.bagen.dwebbrowser.microService.browser.jmm.JmmMetadata
import info.bagen.dwebbrowser.microService.browser.jmm.JmmNMM.Companion.getAndUpdateJmmNmmApps
import info.bagen.dwebbrowser.microService.browser.jmm.JsMicroModule
import info.bagen.dwebbrowser.microService.browser.jmm.getOrPutOrReplace

class CotDemoJMM : JsMicroModule(
  JmmMetadata(
    id = "game.dweb.waterbang.top.dweb",  // TODO warning 不能写大写
    version = "1.0.0",
    server = JmmMetadata.MainServer(
      root = "/usr",
      entry = "public.service.worker.js"
    ),
    icon = "https://www.bfmeta.info/imgs/logo3.webp",
    name = "game"
  )
) {
  init {
    // TODO 测试打开的需要把metadata添加到 jmm app
    getAndUpdateJmmNmmApps()[mmid] = this
  }
}