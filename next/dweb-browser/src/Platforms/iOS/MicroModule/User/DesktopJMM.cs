using DwebBrowser.MicroService.Browser.Jmm;
namespace DwebBrowser.MicroService.User;

public class DesktopJMM : JsMicroModule
{
    public DesktopJMM() : base(new JmmMetadata(
        id: "desktop.user.dweb",
        version: "1.0.0",
        server: new JmmMetadata.MainServer() { Root = "file:///jmm", Entry = "/desktop.worker.js" },
        icon: "https://www.bfmeta.info/imgs/logo3.webp",
        name: "desktop")
        )
    {
    }
}

