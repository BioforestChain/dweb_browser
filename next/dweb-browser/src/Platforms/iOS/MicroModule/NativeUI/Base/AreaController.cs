using System;
using UIKit;

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
    public bool Overlay;
    public AreaJson Area;
}

public static class CGRectExtensions
{
    public static AreaJson ToAreaJson(this CoreGraphics.CGRect rect) => new((float)rect.Top, (float)rect.Left, (float)rect.Right, (float)rect.Bottom);
}