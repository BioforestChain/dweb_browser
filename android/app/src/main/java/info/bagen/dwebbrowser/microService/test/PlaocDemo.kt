package info.bagen.dwebbrowser.microService.test

import info.bagen.dwebbrowser.microService.browser.jmm.JmmMetadata
import info.bagen.dwebbrowser.microService.browser.jmm.JsMicroModule

class PlaocDemoJMM : JsMicroModule(
  JmmMetadata(
    id = "plaoc.dweb-browser.org.dweb",
    server = JmmMetadata.MainServer("/sys", "/server/plaoc.server.js"),
    name = "plaoc-demo",
    short_name = "demo",
    icon = "https://www.bfmeta.info/imgs/logo3.webp",
    release_date = "Sun Jun 25 2023 18:28:25 GMT+0800 (China Standard Time)",
    images = listOf(
      "http://qiniu-waterbang.waterbang.top/bfm/cot-home_2058.webp",
      "http://qiniu-waterbang.waterbang.top/bfm/defi.png",
      "http://qiniu-waterbang.waterbang.top/bfm/nft.png"
    ),
    author = listOf("bfs", "bfs@bfs.com"),
    version = "1.0.8",
    categories = listOf("demo", "vue3"),
    home = "https://dweb.waterbang.top",
  )
) {
}