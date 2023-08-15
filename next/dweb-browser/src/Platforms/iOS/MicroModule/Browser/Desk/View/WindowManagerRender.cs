
namespace DwebBrowser.MicroService.Browser.Desk;

public partial class DesktopWindowsManager
{
	public Task Render() => MainThread.InvokeOnMainThreadAsync(() =>
	{
		foreach (var win in WinList.Get())
		{

		}
	});
}

