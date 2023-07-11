using DwebBrowser.Base;
using DwebBrowser.MicroService.Http;

namespace DwebBrowser.MicroService.Browser;

public class BrowserController : BaseViewController
{
    public BrowserNMM BrowserNMM { get; init; }

    public BrowserController(BrowserNMM browserNMM)
    {
        BrowserNMM = browserNMM;
    }

    public Task<PureResponse> OpenJMM(Mmid mmid) =>
        BrowserNMM.NativeFetchAsync(new URL("file://jmm.browser.dweb/openApp")
            .SearchParamsSet("app_id", mmid));

    public Task<PureResponse> CloseJMM(Mmid mmid) =>
        BrowserNMM.NativeFetchAsync(new URL("file://jmm.browser.dweb/closeApp")
            .SearchParamsSet("app_id", mmid));
}

