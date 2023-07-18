using DwebBrowser.Base;
using UIKit;
using WebKit;

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

    private static readonly Dictionary<Mmid, UIViewController> s_activityMap = new();

    protected UIViewController? _getActivity(Mmid mmid) => s_activityMap.GetValueOrDefault(mmid);

    public abstract void OpenActivity(Mmid remoteMmid);

    private readonly HashSet<Signal<Mmid, BaseViewController>> _ActivitySignal = new();
    public event Signal<Mmid, BaseViewController> _OnActivity
    {
        add { if(value != null) lock (_ActivitySignal) { _ActivitySignal.Add(value); } }
        remove { lock (_ActivitySignal) { _ActivitySignal.Remove(value); } }
    }
    protected Task _OnActivityEmit(Mmid mmid, BaseViewController controller) => (_ActivitySignal.Emit(mmid, controller)).ForAwait();

    public static readonly PromiseOut<UIWindow> Window = new();
    public static readonly PromiseOut<UINavigationController> RootViewController = new();
}

