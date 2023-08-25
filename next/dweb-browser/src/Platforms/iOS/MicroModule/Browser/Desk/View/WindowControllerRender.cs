using CoreGraphics;
using UIKit;

namespace DwebBrowser.MicroService.Core;

public partial class WindowController
{
    public async Task Render()
    {
        var bounds = UIScreen.MainScreen.Bounds;
        var win = this;

        /// 窗口是否在移动中
        var inMove = win.InMove.Get();

        /// 窗口是否在调整大小中
        var inResize = win.InResize.Get();

        var limits = new WindowLimits(
            MinWidth: (float)bounds.Width * 0.2f,
            MinHeight: (float)bounds.Height * 0.2f,
            MaxWidth: (float)bounds.Width,
            MaxHeight: (float)bounds.Height,
            MinScale: 0.3,
            TopBarBaseHeight: 54f,
            BottomBarBaseHeight: 34f
            );

        var winBounds = win.CalcWindowBoundsByLimits(limits);

        await WindowAdapterManager.Instance.RenderProviders.GetValueOrDefault(win.Id).Invoke(
            new WindowRenderScope(winBounds, (float)limits.MinScale), win);
    }
}

