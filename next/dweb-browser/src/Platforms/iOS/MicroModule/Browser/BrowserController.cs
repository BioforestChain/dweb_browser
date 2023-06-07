using DwebBrowser.Base;
using DwebBrowser.MicroService.Browser.Jmm;

namespace DwebBrowser.MicroService.Browser;

public class BrowserController : BaseViewController
{
    static readonly Debugger Console = new("BrowserController");
    public BrowserNMM BrowserNMM { get; init; }

    public BrowserController(BrowserNMM browserNMM)
    {
        BrowserNMM = browserNMM;
    }

    public State<bool> ShowLoading = new(false);

    private Dictionary<Mmid, Ipc> _openIpcMap = new();

    public async Task OpenApp(Mmid mmid)
    {
        //ShowLoading.Set(true);
        var ipc = await _openIpcMap.GetValueOrPutAsync(mmid, async () =>
        {
            var connectResult = await BrowserNMM.ConnectAsync(mmid);
            connectResult.IpcForFromMM.OnEvent += async (Event, _, _) =>
            {
                if (Event.Name == EIpcEvent.Ready.Event)
                {
                    //BrowserNMM.BrowserController?.ShowLoading.Set(false);
                    Console.Log("openApp", "event::{0} ==> {1} from ==> {2}", Event.Name, Event.Data, mmid);
                }
            };
            return connectResult.IpcForFromMM;
        });

        Console.Log("openApp", "postMessage ==> activity {0}, {1}", mmid, ipc.Remote.Mmid);
        await ipc.PostMessageAsync(IpcEvent.FromUtf8(EIpcEvent.Activity.Event, ""));
    }

    public Task UninstallJMM(JmmMetadata jmmMetadata) =>
        BrowserNMM.NativeFetchAsync(new URL("file://jmm.browser.dweb/uninstall")
            .SearchParamsSet("mmid", jmmMetadata.Id));
}

