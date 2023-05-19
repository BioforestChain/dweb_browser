using UIKit;
using CoreGraphics;
using System.Text.Json;
using DwebBrowser.MicroService.Sys.Mwebview;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.Base;

namespace DwebBrowser.Platforms.iOS.MicroModule.NativeUI.NavigationBar;

public class NavigationBarController : BarController, IToJsonAble
{
    static Debugger Console = new("NavigationBarController");
    public readonly State<NavigationBarState> Observer;
    public StateObservable<NavigationBarState> StateObserver;

    public NavigationBarController(MultiWebViewController mwebviewController) : base(
        colorState: new(mwebviewController.NavigationBarView.BackgroundColor.ToColor()),
        styleState: new(mwebviewController.StatusBarStyle),
        visibleState: new(!mwebviewController.NavigationBarView.Hidden),
        overlayState: new(mwebviewController.NavigationBarView.Alpha >= 1 ? false : true),
        areaState: new(mwebviewController.NavigationBarView.Frame.ToAreaJson()))
    {
        Observer = new State<NavigationBarState>(() => GetState());
        StateObserver = new(() => ToJson());

        Observer.OnChange += async (value, oldValue, _) =>
        {
            await MainThread.InvokeOnMainThreadAsync(async () =>
            {
                //Console.Log("OnChange", "visible: {0}, style: {1}, color: {2}, overlay: {3}",
                //    value.Visible, value.Style, value.Color, value.Overlay);

                var currentHidden = mwebviewController.NavigationBarView.Hidden;
                mwebviewController.NavigationBarView.Hidden = !value.Visible;

                var currentStatusBarStyle = mwebviewController.StatusBarStyle;
                mwebviewController.NavigationBarView.Alpha = value.Overlay ? new nfloat(0.5) : 1;

                mwebviewController.NavigationBarView.BackgroundColor = UIColor.FromRGBA(
                    value.Color.red.ToNFloat(),
                    value.Color.green.ToNFloat(),
                    value.Color.blue.ToNFloat(),
                    value.Color.alpha.ToNFloat());

                if (currentHidden == value.Visible)
                {
                    mwebviewController.SetNeedsUpdateOfHomeIndicatorAutoHidden();
                }
            });

            await StateObserver.EmitAsync();
        };
    }

    public NavigationBarState GetState()
    {
        return new NavigationBarState()
        {
            Color = ColorState.Get(),
            Style = StyleState.Get(),
            Visible = VisibleState.Get(),
            Overlay = OverlayState.Get(),
            Area = AreaState.Get()
        };
    }

    public string ToJson()
    {
        return JsonSerializer.Serialize(GetState());
    }
}

public class NavigationBarState : BarState
{ }