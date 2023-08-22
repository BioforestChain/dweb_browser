using UIKit;
using CoreGraphics;


namespace DwebBrowser.MicroService.Browser.Desk;

public partial class DesktopWindowsManager
{
	public Task Render(DeskAppUIView deskAppUIView) => MainThread.InvokeOnMainThreadAsync(async () =>
	{
		foreach (var win in WinList.Get())
		{
			var bounds = win.State.Bounds;

            await win.Render(deskAppUIView, new CGRect(bounds.Left, bounds.Top, bounds.Width, bounds.Height));
		}
	});
}

