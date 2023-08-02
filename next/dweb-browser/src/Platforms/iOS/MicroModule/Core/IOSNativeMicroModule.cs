using DwebBrowser.Base;
using UIKit;

#nullable enable

namespace DwebBrowser.MicroService.Core;

public abstract class IOSNativeMicroModule : NativeMicroModule
{
    public IOSNativeMicroModule(Mmid mmid, string name) : base(mmid, name)
    {
        OnController += async (mmid, controller, _) =>
        {
            s_activityMap.TryAdd(mmid, controller);
            controller.OnDestroyController += async (_) => { s_activityMap.Remove(mmid); };
        };
    }

    private static readonly Dictionary<Mmid, UIViewController> s_activityMap = new();

    protected UIViewController? _getActivity(Mmid mmid) => s_activityMap.GetValueOrDefault(mmid);

    public abstract void OpenActivity(Mmid remoteMmid);

    private readonly HashSet<Signal<Mmid, BaseViewController>> ControllerSignal = new();
    public event Signal<Mmid, BaseViewController> OnController
    {
        add { if (value != null) lock (ControllerSignal) { ControllerSignal.Add(value); } }
        remove { lock (ControllerSignal) { ControllerSignal.Remove(value); } }
    }
    protected Task OnControllerEmit(Mmid mmid, BaseViewController controller) => (ControllerSignal.Emit(mmid, controller)).ForAwait();

    public static readonly PromiseOut<UIWindow> Window = new();
    public static readonly PromiseOut<UINavigationController> RootViewController = new();
}

