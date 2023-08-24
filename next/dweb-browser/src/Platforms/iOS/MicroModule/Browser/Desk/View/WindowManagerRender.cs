using CoreGraphics;


namespace DwebBrowser.MicroService.Browser.Desk;

public partial class DesktopWindowsManager
{
	public Task Render() => MainThread.InvokeOnMainThreadAsync(async () =>
	{
		var subviews = ((DeskController)Controller).GetDeskAppUIViews();

        foreach (var win in WinList.Get())
		{
			/// 新增应用才需要进行render，旧应用只需要修改层级
			if (subviews.Find(it => it.Win.State.Wid == win.Id) is DeskAppUIView deskAppUIView && deskAppUIView is not null)
			{
				deskAppUIView.Layer.ZPosition = win.State.ZIndex;
				continue;
			}
			var bounds = win.State.Bounds;

            await win.Render(new CGRect(bounds.Left, bounds.Top, bounds.Width, bounds.Height));
		}
	});
}

