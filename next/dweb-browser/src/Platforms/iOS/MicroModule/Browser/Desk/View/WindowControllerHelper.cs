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
