using System.Net;
using DwebBrowser.MicroService.Http;

namespace DwebBrowser.MicroService.Sys.Notification;

public class NotificationNMM : NativeMicroModule
{
    public NotificationNMM() : base("notification.sys.dweb", "notification")
    {
    }


    // TODO: 暂时不完成Notification
    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        HttpRouter.AddRoute(IpcMethod.Get, "/create", async (request, _) =>
        {
            return new PureResponse(HttpStatusCode.OK);
        });
    }
}

