using CoreGraphics;
using DwebBrowser.MicroService.Browser.Desk;

namespace DwebBrowser.MicroService.Core;

public partial class WindowController
{
    public async Task Render(DeskAppUIView deskAppUIView, CGRect frame)
    {
        var win = this;

        /// 窗口是否在移动中
        var inMove = win.InMove.Get();

        /// 窗口是否在调整大小中
        var inResize = win.InResize.Get();

        var limits = new WindowLimits(
            MinWidth: (float)frame.Width * 0.2f,
            MinHeight: (float)frame.Height * 0.2f,
            MaxWidth: (float)frame.Width,
            MaxHeight: (float)frame.Height,
            MinScale: 0.3,
            TopBarBaseHeight: 36f,
            BottomBarBaseHeight: 24f
            );

        WindowAdapterManager.Instance.RenderProviders.GetValueOrDefault(win.Id).Invoke(
            new WindowRenderScope(((float)frame.Width), (float)frame.Height, (float)limits.MinScale), deskAppUIView);
    }
}

