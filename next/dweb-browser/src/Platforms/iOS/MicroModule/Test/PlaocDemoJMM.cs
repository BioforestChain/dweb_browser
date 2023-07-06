
using DwebBrowser.MicroService.Browser.Jmm;

namespace DwebBrowser.MicroService.Test;

public class PlaocDemoJMM : JsMicroModule
{
    public PlaocDemoJMM() : base(new JmmMetadata(
        id: "game.dweb.waterbang.top.dweb",
        server: new JmmMetadata.MainServer() { Root = "/sys", Entry = "/server/plaoc.server.js" },
        name: "plaoc-demo",
        short_name: "demo",
        icon: "https://www.bfmeta.info/imgs/logo3.webp",
        release_date: "Sun Jun 25 2023 18:28:25 GMT+0800 (China Standard Time)",
        images: new List<string> {
            "http://qiniu-waterbang.waterbang.top/bfm/cot-home_2058.webp",
          "http://qiniu-waterbang.waterbang.top/bfm/defi.png",
          "http://qiniu-waterbang.waterbang.top/bfm/nft.png"
        },
        author: new List<string> { "bfs", "bfs@bfs.com" },
        version: "1.0.8",
        categories: new List<string> { "demo", "vue3" },
        home: "https://dweb.waterbang.top"
        ))
    {
    }
}

