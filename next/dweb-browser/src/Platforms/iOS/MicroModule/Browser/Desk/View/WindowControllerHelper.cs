using System.Runtime.CompilerServices;

namespace DwebBrowser.MicroService.Core;

public partial class WindowController
{
	public ConditionalWeakTable<WindowController, State<bool>> InMoveStore = new();

    /// <summary>
    /// 窗口是否在移动中
    /// </summary>
    public State<bool> InMove => InMoveStore.GetValueOrPut(this, () => new State<bool>(false));

	public ConditionalWeakTable<WindowController, State<bool>> InResizeStore = new();

    /// <summary>
    /// 窗口是否在调整大小中
    /// </summary>
    public State<bool> InResize => InResizeStore.GetValueOrPut(this, () => new State<bool>(false));

    /// <summary>
    /// 触发 window 聚焦状态的更新事件监听
    /// </summary>
    /// <param name="focused"></param>
    public void EmitFocusOrBlur(bool focused)
    {
        if (focused)
        {
            _ = Focus().NoThrow();
        }
        else
        {
            _ = Blur().NoThrow();
        }
    }

    public WindowBounds CalcWindowBoundsByLimits(WindowLimits limits)
    {
        if (State.Mode == WindowMode.MAXIMIZE)
        {
            InMove.Set(false);
            return State.UpdateBounds(new WindowBounds(0, 0, limits.MaxWidth, limits.MaxHeight));
        }
        else
        {
            var bounds = State.Bounds;
            var winWidth = Math.Max(bounds.Width, limits.MinWidth);
            var winHeight = Math.Max(bounds.Height, limits.MinHeight);

            return State.UpdateBounds(new WindowBounds(bounds.Left, bounds.Top, winWidth, winHeight));
        }
    }
}

public record WindowLimits(
    float MinWidth,
    float MinHeight,
    float MaxWidth,
    float MaxHeight,

    /// <summary>
    /// 窗口的最小缩放
    ///
    /// 和宽高不一样，缩放意味着保持宽高不变的情况下，将网页内容缩小，从而可以展示更多的网页内容
    /// </summary>
    double MinScale,

    /// <summary>
    /// 窗口顶部的基本高度
    /// </summary>
    double TopBarBaseHeight,

    /// <summary>
    /// 窗口底部的基本高度
    /// </summary>
    double BottomBarBaseHeight
    );
