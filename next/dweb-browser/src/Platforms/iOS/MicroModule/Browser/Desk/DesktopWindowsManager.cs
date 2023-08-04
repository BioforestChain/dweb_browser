using System.Runtime.CompilerServices;
using UIKit;

namespace DwebBrowser.MicroService.Browser.Desk;

public class DesktopWindowsManager
{
	private DeskController DeskController { get; init; }

	public DesktopWindowsManager(DeskController deskController)
	{
		DeskController = deskController;

		AllWindows = new ChangeableList<DesktopWindowController>().Also(it =>
		{
			it.OnChange += async (wins, _) =>
			{
				var newWinList = wins.OrderBy(win => win.State.ZIndex).ToList();
				var changed = false;
				var winList = WinList.Get();
				if (newWinList.Count == winList.Count)
				{
					for (var i = 0; i < winList.Count; i++)
					{
						if (winList[i] != newWinList[i])
						{
							changed = true;
						}
					}
				}
				else
				{
					changed = true;
				}

				if (changed)
				{
					WinList.Set(newWinList);
				}
            };
		});

		var offAdapter = WindowAdapterManager.WindowAdapterManagerInstance.Append(async winState =>
		{
			{
                var bounds = UIScreen.MainScreen.Bounds;
                var displayWidth = Convert.ToSingle(bounds.Width);
                var displayHeight = Convert.ToSingle(bounds.Height);
                if (float.IsNaN(winState.Bounds.Width))
                {
                    winState.Bounds.Width = (float)(displayWidth / Math.Sqrt(2));
                }
                if (float.IsNaN(winState.Bounds.Height))
                {
                    winState.Bounds.Height = (float)(displayHeight / Math.Sqrt(3));
                }
                if (float.IsNaN(winState.Bounds.Left))
                {
                    var maxLeft = displayWidth - winState.Bounds.Width;
                    var gapSize = 47f;
                    var gapCount = Convert.ToInt32(maxLeft / gapSize);

                    winState.Bounds.Left = gapSize + AllWindows.Count % gapCount * gapSize;
                }
                if (float.IsNaN(winState.Bounds.Top))
                {
                    var maxTop = displayHeight - winState.Bounds.Height;
                    var gapSize = 71f;
                    var gapCount = Convert.ToInt32(maxTop / gapSize);

                    winState.Bounds.Top = gapSize + AllWindows.Count % gapCount * gapSize;
                }
            }

			var win = new DesktopWindowController(deskController, winState);
			{
                /// 对窗口做一些启动准备
				
            }


			return win;
		});
	}

	private static ConditionalWeakTable<DeskController, DesktopWindowsManager> Instances = new();

	public static DesktopWindowsManager GetInstance(DeskController deskController)
		=> Instances.GetValueOrPut(deskController, () => new DesktopWindowsManager(deskController));

    /// <summary>
    /// 一个已经根据 zIndex 排序完成的只读列表
    /// </summary>
    public readonly State<List<DesktopWindowController>> WinList = new(new List<DesktopWindowController>());

	private ChangeableList<DesktopWindowController> AllWindows { get; init; }

	private DesktopWindowController? LastFocusedWin = null;
}

