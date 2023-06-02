using UIKit;
using CoreGraphics;
using System.Text.Json;
using DwebBrowser.MicroService.Browser.Mwebview;
using DwebBrowser.MicroService.Browser.NativeUI.Base;

namespace DwebBrowser.MicroService.Browser.NativeUI.StatusBar;

public class StatusBarController : BarController, IToJsonAble
{
    static readonly Debugger Console = new("StatusBarController");
    public readonly State<StatusBarState> Observer;
    public StateObservable<StatusBarState> StateObserver { get; init; }
    public NativeUiController NativeUiController { get; init; }

    public StatusBarController(
        MultiWebViewController mwebviewController,
        NativeUiController nativeUiController) : base(
        colorState: new(mwebviewController.StatusBarView.BackgroundColor.ToColor()),
        styleState: new(mwebviewController.StatusBarStyle),
        visibleState: new(!mwebviewController.StatusBarView.Hidden),
        overlayState: new(mwebviewController.StatusBarView.Alpha >= 1 ? false : true),
        areaState: new(mwebviewController.StatusBarView.Frame.ToAreaJson())
    )
    {
        Observer = new State<StatusBarState>(GetState);
        StateObserver = new(ToJson);
        NativeUiController = nativeUiController;

        Observer.OnChange += async (value, oldValue, _) =>
        {
            await MainThread.InvokeOnMainThreadAsync(async () =>
            {
                var currentAlpha = mwebviewController.StatusBarView.Alpha;

                mwebviewController.StatusBarView.Hidden = !value.Visible;
                mwebviewController.StatusBarStyle = value.Style;

                mwebviewController.StatusBarView.Alpha = value.Overlay ? new nfloat(0.5) : 1;

                mwebviewController.StatusBarView.BackgroundColor = UIColor.FromRGBA(
                    value.Color.red.ToNFloat(),
                    value.Color.green.ToNFloat(),
                    value.Color.blue.ToNFloat(),
                    value.Color.alpha.ToNFloat());

                AreaState.Set(value.Overlay ? AreaJson.Empty : new(
                        mwebviewController.StatusBarView.Frame.GetMaxY().Value,
                        0,
                        0,
                        0));

                mwebviewController.SetNeedsStatusBarAppearanceUpdate();

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

    public StatusBarState GetState()
    {
        return new StatusBarState
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

public class StatusBarState : BarState
{
}
