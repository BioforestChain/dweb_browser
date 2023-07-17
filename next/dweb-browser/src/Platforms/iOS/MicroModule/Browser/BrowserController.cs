using DwebBrowser.Base;

namespace DwebBrowser.MicroService.Browser;

public class BrowserController : BaseViewController
{
    public BrowserNMM BrowserNMM { get; init; }

    public BrowserController(BrowserNMM browserNMM)
    {
        BrowserNMM = browserNMM;
    }
}

