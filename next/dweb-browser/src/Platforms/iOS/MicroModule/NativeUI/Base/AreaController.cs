using UIKit;
using System.Text.Json.Serialization;

namespace DwebBrowser.Platforms.iOS.MicroModule.NativeUI.Base;

public abstract class AreaController
{
    /// <summary>
    /// 是否层叠渲染
    /// </summary>
    readonly public State<bool> OverlayState;

    /// <summary>
    /// 插入空间
    /// </summary>
    readonly public State<AreaJson> AreaState;


    public AreaController(State<bool> overlayState, State<AreaJson> areaState)
    {
        OverlayState = overlayState;
        AreaState = areaState;
    }
}

public class AreaState
{
    [JsonPropertyName("overlay")]
    public bool Overlay { get; set; }
    [JsonPropertyName("insets")]
    public AreaJson Area { get; set; }
}

public static class CGRectExtensions
{
    public static AreaJson ToAreaJson(this CoreGraphics.CGRect rect) =>
        new(rect.Top.Value, rect.Left.Value, rect.Right.Value, rect.Bottom.Value);
}