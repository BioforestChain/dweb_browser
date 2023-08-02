using UIKit;

using DwebBrowser.Base;

namespace DwebBrowser.MicroService.Browser.Web;

public class WebBrowserController : BaseViewController
{
    public WebBrowserNMM WebBrowserNMM { get; init; }

    public WebBrowserController(WebBrowserNMM webBrowserNMM)
    {
        WebBrowserNMM = webBrowserNMM;
    }

    public override void ViewDidLoad()
    {
        base.ViewDidLoad();
        View.Frame = UIScreen.MainScreen.Bounds;
    }
}

