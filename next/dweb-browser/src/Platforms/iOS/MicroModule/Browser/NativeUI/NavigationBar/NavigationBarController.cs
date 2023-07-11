using System.Text.Json;
using CoreGraphics;
using DwebBrowser.MicroService.Browser.Mwebview;
using DwebBrowser.MicroService.Browser.NativeUI.Base;
using UIKit;

namespace DwebBrowser.MicroService.Browser.NativeUI.NavigationBar;

public class NavigationBarController : BarController, IToJsonAble
{
    public readonly State<NavigationBarState> Observer;
    public StateObservable<NavigationBarState> StateObserver { get; init; }
    public NativeUiController NativeUiController { get; init; }

    public NavigationBarController(
        MultiWebViewController mwebviewController,
        NativeUiController nativeUiController) : base(
        colorState: new(mwebviewController.NavigationBarView.BackgroundColor.ToColor()),
        styleState: new(mwebviewController.StatusBarStyle),
        visibleState: new(!mwebviewController.NavigationBarView.Hidden),
        overlayState: new(mwebviewController.NavigationBarView.Alpha < 1),
        areaState: new(mwebviewController.NavigationBarView.Frame.ToAreaJson()))
    {
        Observer = new(GetState);
        StateObserver = new(ToJson);
        NativeUiController = nativeUiController;

        Observer.OnChange += async (value, oldValue, _) =>
        {
            await MainThread.InvokeOnMainThreadAsync(async () =>
            {
                var currentAlpha = mwebviewController.NavigationBarView.Alpha;

                var currentHidden = mwebviewController.NavigationBarView.Hidden;
                mwebviewController.NavigationBarView.Hidden = !value.Visible;

                var currentStatusBarStyle = mwebviewController.StatusBarStyle;
                mwebviewController.NavigationBarView.Alpha = value.Overlay ? new nfloat(0.5) : 1;

                AreaState.Set(value.Overlay ? AreaJson.Empty : new(
                    0,
                    0,
                    0,
                    mwebviewController.NavigationBarView.Frame.GetMaxY().Value
                    - mwebviewController.NavigationBarView.Frame.GetMinY().Value));

                mwebviewController.NavigationBarView.BackgroundColor = UIColor.FromRGBA(
                    value.Color.red.ToNFloat(),
                    value.Color.green.ToNFloat(),
                    value.Color.blue.ToNFloat(),
                    value.Color.alpha.ToNFloat());

                if (currentHidden == value.Visible)
                {
                    mwebviewController.SetNeedsUpdateOfHomeIndicatorAutoHidden();
                }

                // 如果overlay有变化，主动通知SafeArea
                if ((currentAlpha < 1 && !value.Overlay) || (currentAlpha >= 1 && value.Overlay))
                {
                    NativeUiController.SafeArea.AreaState.Set(new(0, 0, 0, 0));
                    NativeUiController.SafeArea.Observer.Get();
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