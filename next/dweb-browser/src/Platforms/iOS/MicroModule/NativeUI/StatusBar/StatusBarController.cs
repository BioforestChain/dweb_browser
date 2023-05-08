using UIKit;
using System;
using DwebBrowser.Platforms.iOS.MicroModule.NativeUI.Base;
using System.Text.Json;

namespace DwebBrowser.Platforms.iOS.MicroModule.NativeUI.StatusBar;

public class StatusBarController : BarController, IToJsonAble
{
    public readonly State<StatusBarState> Observer;
    public StateObservable<StatusBarState> StateObserver;

    public StatusBarController(UIApplication app) : base(
        colorState: new(app.StatusBarStyle.ToColor()),
        styleState: new(app.StatusBarStyle.ToBarStyle()),
        visibleState: new(app.StatusBarHidden),
        overlayState: new(true),
        areaState: new(app.StatusBarFrame.ToAreaJson())
    )
    {
        Observer = new State<StatusBarState>(() => GetState());
        StateObserver = new(Observer, () => ToJson());
    }
    public StatusBarController() : this(UIApplication.SharedApplication) { }



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