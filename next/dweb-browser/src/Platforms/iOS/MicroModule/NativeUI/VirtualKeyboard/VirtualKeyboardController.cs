using UIKit;
using Foundation;
using System.Text.Json;
using System.Text.Json.Serialization;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.Base;

namespace DwebBrowser.Platforms.iOS.MicroModule.NativeUI.VirtualKeyboard;

public class VirtualKeyboardController : AreaController, IToJsonAble
{
    public readonly State<VirtualKeyboardState> Observer;
    public readonly State<bool> VisibleState;

    // TODO: 未验证Keyboard
    public VirtualKeyboardController() : base(new(false), new(new AreaJson(0, 0, 0, 0)))
    {
        UIKeyboard.Notifications.ObserveWillShow((sender, args) =>
        {
            VisibleState.Set(true);
            AreaState.Set(args.FrameBegin.ToAreaJson());

        });

        UIKeyboard.Notifications.ObserveWillHide((sender, args) =>
        {
            VisibleState.Set(false);
            AreaState.Set(args.FrameEnd.ToAreaJson());
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