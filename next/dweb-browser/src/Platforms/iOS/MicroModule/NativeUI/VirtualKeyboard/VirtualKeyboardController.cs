using UIKit;
using Foundation;
using System.Text.Json;
using System.Text.Json.Serialization;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.Base;

namespace DwebBrowser.Platforms.iOS.MicroModule.NativeUI.VirtualKeyboard;

public class VirtualKeyboardController : IToJsonAble
{
    public readonly State<VirtualKeyboardState> Observer;
    public State<bool> VisibleState;
    public State<bool> OverlayState;
    public State<AreaJson> AreaState;

    // TODO: 未验证Keyboard
    public VirtualKeyboardController()
    {
        UIKeyboard.Notifications.ObserveWillShow((sender, args) =>
        {
            VisibleState = new(true);
            OverlayState = new(true);
            AreaState = new(args.FrameBegin.ToAreaJson());
        });

        UIKeyboard.Notifications.ObserveWillHide((sender, args) =>
        {
            VisibleState = new(false);
            OverlayState = new(false);
            AreaState = new(args.FrameEnd.ToAreaJson());
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