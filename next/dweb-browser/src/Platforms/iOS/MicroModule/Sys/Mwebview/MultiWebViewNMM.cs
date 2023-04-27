using DwebBrowser.MicroService;
using DwebBrowser.MicroService.Core;
using DwebBrowser.MicroService.Message;
using System.Net;
using UIKit;

namespace DwebBrowser.MicroService.Sys.Mwebview;

public class MultiWebViewNMM : IOSNativeMicroModule
{
    public MultiWebViewNMM() : base("mwebview.sys.dweb")
    {
    }

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        /// nativeui 与 mwebview 是伴生关系
        bootstrapContext.Dns.BootstrapAsync("nativeui.sys.dweb");

        // 打开一个webview作为窗口
        HttpRouter.AddRoute(IpcMethod.Get, "/open", async (request, ipc) =>
        {
            var url = request.QueryValidate<string>("url")!;
            var remoteMM = ipc.AsRemoteInstance() ?? throw new Exception("mwebview.sys.dweb/open should be call by locale");


            return new HttpResponseMessage(HttpStatusCode.OK);
        });

        //HttpRouter.AddRoute(IpcMethod.Get, "/close", async (request, ipc) =>
        //{

        //});
    }

    public override void OpenActivity(string remoteMmid)
    {
        
    }
}

