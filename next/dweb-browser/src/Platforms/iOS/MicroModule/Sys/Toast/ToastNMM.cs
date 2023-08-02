using DwebBrowser.MicroService.Browser.Mwebview;

namespace DwebBrowser.MicroService.Sys.Toast;

public class ToastNMM : NativeMicroModule
{
    public ToastNMM() : base("toast.sys.dweb", "toast")
    {
    }

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        HttpRouter.AddRoute(IpcMethod.Get, "/show", async (request, ipc) =>
        {
            var message = request.QueryStringRequired("message");
            var durationType = request.QueryString("duration") switch
            {
                string d when d.EqualsIgnoreCase("LONG") => ToastController.ToastDuration.LONG,
                _ => ToastController.ToastDuration.SHORT
            };
            var positionType = request.QueryString("position") switch
            {
                string p when p.EqualsIgnoreCase("TOP") => ToastController.ToastPosition.TOP,
                string p when p.EqualsIgnoreCase("CENTER") => ToastController.ToastPosition.CENTER,
                _ => ToastController.ToastPosition.BOTTOM
            };

            var mwebViewController = MultiWebViewNMM.GetCurrentWebViewController(ipc.Remote.Mmid);
            await ToastController.ShowToastAsync(message, mwebViewController, durationType, positionType);

            return true;
        });
    }
}

