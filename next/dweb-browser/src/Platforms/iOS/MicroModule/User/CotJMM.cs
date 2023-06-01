using DwebBrowser.MicroService.Sys.Jmm;

namespace DwebBrowser.MicroService.Sys.User;

public class CotJMM : JsMicroModule
{
    public CotJMM() : base(new JmmMetadata(
        "cot.bfs.dweb",
        new JmmMetadata.MainServer() { Root = "file:///jmm", Entry = "/cot.worker.js" }))
    {
        // TODO 测试打开的需要把metadata添加到 jmm app
        JmmNMM.GetAndUpdateJmmNmmApps().Add(Mmid, this);
    }
}

