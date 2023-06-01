﻿using DwebBrowser.MicroService.Sys.Jmm;
namespace DwebBrowser.MicroService.Sys.User;

public class CotDemoJMM : JsMicroModule
{
    public CotDemoJMM() : base(new JmmMetadata(
        id: "game.dweb.waterbang.top.dweb",
        version: "1.0.0",
        server: new JmmMetadata.MainServer() { Root = "file:///jmm", Entry = "/public.service.worker.js" },
        icon: "https://www.bfmeta.info/imgs/logo3.webp",
        title: "game"
))
    {
        JmmNMM.GetAndUpdateJmmNmmApps().Add(Mmid, this);
    }
}

