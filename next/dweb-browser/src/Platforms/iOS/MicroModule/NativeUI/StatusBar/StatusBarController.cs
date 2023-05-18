using UIKit;
using CoreGraphics;
using System.Text.Json;
using DwebBrowser.MicroService.Sys.Mwebview;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.Base;

namespace DwebBrowser.Platforms.iOS.MicroModule.NativeUI.StatusBar;

public class StatusBarController : BarController, IToJsonAble
{
    static Debugger Console = new("StatusBarController");
    public readonly State<StatusBarState> Observer;
    public StateObservable<StatusBarState> StateObserver;

    public StatusBarController(MultiWebViewController mwebviewController) : base(
        colorState: new(mwebviewController.StatusBarView.BackgroundColor.ToColor()),
        styleState: new(mwebviewController.PreferredStatusBarStyle().ToBarStyle()),
        visibleState: new(!mwebviewController.StatusBarView.Hidden),
        overlayState: new(mwebviewController.StatusBarView.Alpha >= 1 ? false : true),
        areaState: new(mwebviewController.StatusBarView.Frame.ToAreaJson())
    )
    {
        Observer = new State<StatusBarState>(() => GetState());
        StateObserver = new(() => ToJson());

        Observer.OnChange += async (value, oldValue, _) =>
        {
            await MainThread.InvokeOnMainThreadAsync(async () =>
            {
                //Console.Log("OnChange", "visible: {0}, style: {1}, color: {2}, overlay: {3}",
                //    value.Visible, value.Style, value.Color, value.Overlay);

                mwebviewController.StatusBarView.Hidden = !value.Visible;
                mwebviewController.StatusBarStyle = value.Style;
                mwebviewController.StatusBarView.Alpha = value.Overlay ? new nfloat(0.5) : 1;

                mwebviewController.StatusBarView.BackgroundColor = UIColor.FromRGBA(
                    value.Color.red.ToNFloat(),
                    value.Color.green.ToNFloat(),
                    value.Color.blue.ToNFloat(),
                    value.Color.alpha.ToNFloat());

                //Console.Log("BackgroundColor", "red: {0}, green: {1}, blue: {2}, alpha: {3}",
                //    value.Color.red.ToNFloat(),
                //    value.Color.green.ToNFloat(),
                //    value.Color.blue.ToNFloat(),
                //    value.Color.alpha.ToNFloat());

                //var window = await MultiWebViewNMM.Window.WaitPromiseAsync();

                //var frame = window.WindowScene.StatusBarManager.StatusBarFrame;
                ////Console.Log("Frame", "bottom: {0}, top: {1}, left: {2}, right: {3}",
                ////    frame.Bottom.Value, frame.Top.Value, frame.Left.Value, frame.Right.Value);

                ///// TODO：当隐藏Frame之后，无法读取到系统StatusBar的Frame的大小，先刷新一次页面可以获取到
                ///// 是否应该先存储显示时的Frame用于再显示时的赋值？
                //if (mwebviewController.StatusBarView.Frame == CGRect.Empty && value.Visible)
                //{
                //    mwebviewController.SetNeedsStatusBarAppearanceUpdate();
                //}

                //mwebviewController.StatusBarView.Frame = value.Visible
                //    ? window.WindowScene.StatusBarManager.StatusBarFrame
                //    : CGRect.Empty;

                mwebviewController.SetNeedsStatusBarAppearanceUpdate();
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
