using DwebBrowser.Base;

namespace DwebBrowser.MicroService.Browser.Web;

public class WebBrowserController : BaseViewController
{
    public WebBrowserNMM WebBrowserNMM { get; init; }

    public WebBrowserController(WebBrowserNMM webBrowserNMM)
    {
        WebBrowserNMM = webBrowserNMM;
    }
}

