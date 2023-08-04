using DwebBrowser.MicroService.Browser.Desk;
using UIKit;

#nullable enable

namespace DwebBrowser.MicroService.Core;

public abstract class IOSNativeMicroModule : NativeMicroModule
{
    public IOSNativeMicroModule(Mmid mmid, string name) : base(mmid, name)
    {
    }

    public static readonly PromiseOut<UIWindow> Window = new();
    public static readonly PromiseOut<UINavigationController> RootViewController = new();

    private static DeskController? DeskController = null;

    public async Task<DeskController?> GetDeskController()
    {
        if (DeskController is not null)
        {
            return DeskController;
        }

        var vc = await RootViewController.WaitPromiseAsync();

        foreach (var controller in vc.ChildViewControllers)
        {
            if (controller is DeskController deskController)
            {
                DeskController = deskController;
                return DeskController;
            }
        }

        return null;
    }
}

