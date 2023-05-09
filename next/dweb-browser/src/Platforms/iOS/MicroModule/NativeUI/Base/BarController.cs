using CoreGraphics;
using UIKit;
using System.Text.Json.Serialization;

namespace DwebBrowser.Platforms.iOS.MicroModule.NativeUI.Base;

public abstract class BarController : AreaController
{
    /// <summary>
    /// 背景色
    /// </summary>
    readonly public State<ColorJson> ColorState;


    /// <summary>
    /// 前景风格
    /// </summary>
    readonly public State<BarStyle> StyleState;

    /// <summary>
    /// 是否可见
    /// </summary>
    readonly public State<bool> VisibleState;

    protected BarController(State<ColorJson> colorState,
                            State<BarStyle> styleState,
                            State<bool> visibleState,
                            State<bool> overlayState,
                            State<AreaJson> areaState) : base(overlayState, areaState)
    {
        this.ColorState = colorState;
        this.StyleState = styleState;
        this.VisibleState = visibleState;
    }
}

public static class StatusBarStyleExtionsions
{
    /// <summary>
    /// IOS 状态栏默认是透明的吧
    /// </summary>
    /// <param name="style"></param>
    /// <returns></returns>
    public static ColorJson ToColor(this UIStatusBarStyle style) => ColorJson.Transparent;
    public static BarStyle ToBarStyle(this UIStatusBarStyle style) => style switch
    {
        UIStatusBarStyle.DarkContent => BarStyle.DarkContent,
        UIStatusBarStyle.LightContent => BarStyle.LightContent,
        _ => BarStyle.Default
    };
}

public class BarState : AreaState
{
    [JsonPropertyName("visible")]
    public bool Visible { get; set; }
    [JsonPropertyName("color")]
    public ColorJson Color { get; set; }
    [JsonPropertyName("style")]
    public BarStyle Style { get; set; }
}
public sealed record BarStyle(string style)
{
    public static BarStyle DarkContent = new("DARK");
    public static BarStyle LightContent = new("LIGHT");
    public static BarStyle Default = new("DEFAULT");
}

