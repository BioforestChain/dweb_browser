using DwebBrowser.WebModule.Jmm;
namespace DwebBrowser.WebModule.User;

public class DesktopJMM : JsMicroModule
{
    public DesktopJMM() : base(new JmmMetadata(
        "desktop.user.dweb",
        new JmmMetadata.MainServer() { Root = "file:///bundle", Entry = "/desktop.worker.js" }))
    {
    }
}

