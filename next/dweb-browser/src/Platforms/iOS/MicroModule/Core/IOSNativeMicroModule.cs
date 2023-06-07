using UIKit;
using DwebBrowser.Base;

#nullable enable

namespace DwebBrowser.MicroService.Core;

public abstract class IOSNativeMicroModule : NativeMicroModule
{
    public IOSNativeMicroModule(Mmid mmid) : base(mmid)
    {
        _OnActivity += async (mmid, controller, _) =>
        {
            s_activityMap.TryAdd(mmid, controller);
            controller.OnDestroyController += async (_) => { s_activityMap.Remove(mmid); };
        };
    }

    private static Dictionary<Mmid, UIViewController> s_activityMap = new();

    protected UIViewController? _getActivity(Mmid mmid) => s_activityMap.GetValueOrDefault(mmid);

    public abstract void OpenActivity(Mmid remoteMmid);

    protected event Signal<Mmid, BaseViewController> _OnActivity;
    protected Task _OnActivityEmit(Mmid mmid, BaseViewController controller) => (_OnActivity?.Emit(mmid, controller)).ForAwait();

    public static readonly PromiseOut<UIWindow> Window = new();
    public static readonly PromiseOut<UINavigationController> RootViewController = new();
}

