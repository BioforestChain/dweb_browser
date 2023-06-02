using DwebBrowser.MicroService.Browser.Jmm;
namespace DwebBrowser.MicroService.User;

public class ToyJMM : JsMicroModule
{
    public ToyJMM() : base(new JmmMetadata(
        "toy.bfs.dweb",
        new JmmMetadata.MainServer() { Root = "file:///jmm", Entry = "/toy.worker.js" }))
    {
    }
}

