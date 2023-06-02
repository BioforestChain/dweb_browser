using DwebBrowser.MicroService.Browser.Jmm;
namespace DwebBrowser.MicroService.User;

public class DesktopJMM : JsMicroModule
{
    public DesktopJMM() : base(new JmmMetadata(
        "desktop.user.dweb",
        new JmmMetadata.MainServer() { Root = "file:///jmm", Entry = "/desktop.worker.js" }))
    {
    }
}

