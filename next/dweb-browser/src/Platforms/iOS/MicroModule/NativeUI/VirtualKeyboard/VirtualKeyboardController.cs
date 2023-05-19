using UIKit;
using Foundation;
using CoreGraphics;
using System.Text.Json;
using System.Text.Json.Serialization;
using DwebBrowser.MicroService.Sys.Mwebview;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.Base;
using DwebBrowser.Helper;

namespace DwebBrowser.Platforms.iOS.MicroModule.NativeUI.VirtualKeyboard;

public class VirtualKeyboardController : AreaController, IToJsonAble
{
    static Debugger Console = new("VirtualKeyboardController");
    public State<VirtualKeyboardState> Observer;
    public StateObservable<VirtualKeyboardState> StateObserver;
    public readonly State<bool> VisibleState = new(false);

    // TODO: 未验证Keyboard
    public VirtualKeyboardController(MultiWebViewController mwebviewController) : base(
            overlayState: new(mwebviewController.VirtualKeyboardView.Alpha >= 1 ? false : true),
            areaState: new(mwebviewController.VirtualKeyboardView.Frame.ToAreaJson()))
    {
        Observer = new(() => GetState());
        StateObserver = new(() => ToJson());

        Observer.OnChange += async (value, oldValue, _) =>
        {
            await MainThread.InvokeOnMainThreadAsync(async () =>
            {
                Console.Log("OnChange", "visible: {0}, overlay: {1}",
                    value.Visible, value.Overlay);
                mwebviewController.VirtualKeyboardView.Hidden = !value.Visible;
                mwebviewController.VirtualKeyboardView.Alpha = value.Overlay ? new nfloat(0.5) : 1;
            });

            await StateObserver.EmitAsync();
        };

        UIKeyboard.Notifications.ObserveWillShow((sender, args) =>
        {
            /// 如果不进行判断，会导致触发3次
            if (mwebviewController.VirtualKeyboardView.Hidden == true)
            {
                VisibleState.Set(true);
                //AreaState.Update(cache => cache = args.FrameEnd.ToAreaJson());

                Console.Log("ObserveWillShow", "FrameBegin top: {0}, bottom: {1}, left: {2}, right: {3}",
                    args.FrameBegin.Top.Value,
                    args.FrameBegin.Bottom.Value,
                    args.FrameBegin.Left.Value,
                    args.FrameBegin.Right.Value);
                Console.Log("ObserveWillShow", "FrameEnd top: {0}, bottom: {1}, left: {2}, right: {3}",
                    args.FrameEnd.Top.Value,
                    args.FrameEnd.Bottom.Value,
                    args.FrameEnd.Left.Value,
                    args.FrameEnd.Right.Value);
                mwebviewController.VirtualKeyboardView.Frame = args.FrameEnd;
                Observer.Get();
            }
        });

        UIKeyboard.Notifications.ObserveWillHide((sender, args) =>
        {
            /// 如果不进行判断，会导致触发3次
            if (mwebviewController.VirtualKeyboardView.Hidden == false)
            {
                VisibleState.Set(false);
                //AreaState.Update(cache => cache = args.FrameEnd.ToAreaJson());
                Console.Log("ObserveWillHide", "FrameBegin top: {0}, bottom: {1}, left: {2}, right: {3}",
                    args.FrameBegin.Top.Value,
                    args.FrameBegin.Bottom.Value,
                    args.FrameBegin.Left.Value,
                    args.FrameBegin.Right.Value);
                Console.Log("ObserveWillHide", "FrameEnd top: {0}, bottom: {1}, left: {2}, right: {3}",
                    args.FrameEnd.Top.Value,
                    args.FrameEnd.Bottom.Value,
                    args.FrameEnd.Left.Value,
                    args.FrameEnd.Right.Value);
                mwebviewController.VirtualKeyboardView.Frame = args.FrameEnd;
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