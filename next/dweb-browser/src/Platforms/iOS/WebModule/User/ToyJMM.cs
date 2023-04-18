using DwebBrowser.WebModule.Jmm;
namespace DwebBrowser.WebModule.User;

public class ToyJMM : JsMicroModule
{
    public ToyJMM() : base(new JmmMetadata(
        "toy.bfs.dweb",
        new JmmMetadata.MainServer() { Root = "file:///bundle", Entry = "/toy.worker.js" }))
    {
    }
}

