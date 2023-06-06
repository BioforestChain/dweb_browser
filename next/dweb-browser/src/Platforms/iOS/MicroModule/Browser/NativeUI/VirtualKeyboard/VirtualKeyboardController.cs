using UIKit;
using CoreGraphics;
using System.Text.Json;
using System.Text.Json.Serialization;
using DwebBrowser.MicroService.Browser.Mwebview;
using DwebBrowser.MicroService.Browser.NativeUI.Base;

namespace DwebBrowser.MicroService.Browser.NativeUI.VirtualKeyboard;

public class VirtualKeyboardController : AreaController, IToJsonAble
{
    static readonly Debugger Console = new("VirtualKeyboardController");
    public readonly State<VirtualKeyboardState> Observer;
    public StateObservable<VirtualKeyboardState> StateObserver { get; init; }
    public readonly State<bool> VisibleState = new(false);
    public NativeUiController NativeUiController { get; init; }

    // TODO: 未验证Keyboard
    public VirtualKeyboardController(
        MultiWebViewController mwebviewController,
        NativeUiController nativeUiController) : base(
            overlayState: new(mwebviewController.VirtualKeyboardView.Alpha >= 1 ? false : true),
            areaState: new(mwebviewController.VirtualKeyboardView.Frame.ToAreaJson()))
    {
        Observer = new(GetState);
        StateObserver = new(ToJson);
        NativeUiController = nativeUiController;

        Observer.OnChange += async (value, oldValue, _) =>
        {
            await MainThread.InvokeOnMainThreadAsync(async () =>
            {
                var currentAlpha = mwebviewController.VirtualKeyboardView.Alpha;
                mwebviewController.VirtualKeyboardView.Hidden = !value.Visible;
                mwebviewController.VirtualKeyboardView.Alpha = value.Overlay ? new nfloat(0.5) : 1;

                // 如果overlay有变化，主动通知SafeArea
                if ((currentAlpha < 1 && !value.Overlay) || (currentAlpha >= 1 && value.Overlay))
                {
                    nativeUiController.SafeArea.AreaState.Set(new(0, 0, 0, 0));
                    nativeUiController.SafeArea.Observer.Get();
                }
            });

            await StateObserver.EmitAsync();
        };

        UIKeyboard.Notifications.ObserveWillShow((sender, args) =>
        {
            /// 如果不进行判断，会导致触发3次
            if (mwebviewController.VirtualKeyboardView.Hidden == true)
            {
                VisibleState.Set(true);

                //Console.Log("ObserveWillShow", "FrameBegin top: {0}, bottom: {1}, left: {2}, right: {3}",
                //    args.FrameBegin.Top.Value,
                //    args.FrameBegin.Bottom.Value,
                //    args.FrameBegin.Left.Value,
                //    args.FrameBegin.Right.Value);
                //Console.Log("ObserveWillShow", "FrameEnd top: {0}, bottom: {1}, left: {2}, right: {3}",
                //    args.FrameEnd.Top.Value,
                //    args.FrameEnd.Bottom.Value,
                //    args.FrameEnd.Left.Value,
                //    args.FrameEnd.Right.Value);
                mwebviewController.VirtualKeyboardView.Frame = args.FrameEnd;
                AreaState.Set(new(
                    0,
                    0,
                    0,
                    args.FrameEnd.Bottom.Value - args.FrameEnd.Top.Value));

                Observer.Get();
            }
        });

        UIKeyboard.Notifications.ObserveWillHide((sender, args) =>
        {
            /// 如果不进行判断，会导致触发3次
            if (mwebviewController.VirtualKeyboardView.Hidden == false)
            {
                VisibleState.Set(false);
                //Console.Log("ObserveWillHide", "FrameBegin top: {0}, bottom: {1}, left: {2}, right: {3}",
                //    args.FrameBegin.Top.Value,
                //    args.FrameBegin.Bottom.Value,
                //    args.FrameBegin.Left.Value,
                //    args.FrameBegin.Right.Value);
                //Console.Log("ObserveWillHide", "FrameEnd top: {0}, bottom: {1}, left: {2}, right: {3}",
                //    args.FrameEnd.Top.Value,
                //    args.FrameEnd.Bottom.Value,
                //    args.FrameEnd.Left.Value,
                //    args.FrameEnd.Right.Value);
                mwebviewController.VirtualKeyboardView.Frame = CGRect.Empty;
                AreaState.Set(AreaJson.Empty);
                Observer.Get();
            }
        });
    }

    public VirtualKeyboardState GetState()
    {
        return new VirtualKeyboardState
        {
            Visible = VisibleState.Get(),
            Overlay = OverlayState.Get(),
            Area = AreaState.Get()
        };
    }

    public string ToJson() => JsonSerializer.Serialize(GetState());
}

public class VirtualKeyboardState : AreaState
{
    [JsonPropertyName("visible")]
    public bool Visible { get; set; }
}