using DwebBrowser.MicroService.Sys.Mwebview;

namespace DwebBrowser.Platforms.iOS.MicroModule.Plugin.Toast;

public class ToastNMM: NativeMicroModule
{
	public ToastNMM(): base("toast.sys.dweb")
	{
	}

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        HttpRouter.AddRoute(IpcMethod.Get, "/show", async (request, ipc) =>
        {
            var duration = request.QueryValidate<string>("duration", false);
            var message = request.QueryValidate<string>("message")!;
            var position = request.QueryValidate<string>("position", false);
            var durationType = duration switch
            {
                string d when d.ToUpper() is "LONG" => ToastController.ToastDuration.LONG,
                _ => ToastController.ToastDuration.SHORT
            };
            var positionType = position switch
            {
                string p when p.ToUpper() is "Top" => ToastController.ToastPosition.TOP,
                string p when p.ToUpper() is "CENTER" => ToastController.ToastPosition.CENTER,
                _ => ToastController.ToastPosition.BOTTOM
            };

            var mwebViewController = MultiWebViewNMM.GetCurrentWebViewController(ipc.Remote.Mmid);
            await ToastController.ShowToastAsync(message, mwebViewController, durationType, positionType);

            return true;
        });
    }
}

