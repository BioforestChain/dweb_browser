using System.Runtime.CompilerServices;
using UIKit;

namespace DwebBrowser.MicroService.Browser.Desk;

public class DesktopWindowsManager
{
	private DeskController DeskController { get; init; }

    private static ConditionalWeakTable<DeskController, DesktopWindowsManager> Instances = new();

    public static DesktopWindowsManager GetInstance(DeskController deskController)
        => Instances.GetValueOrPut(deskController, () => new DesktopWindowsManager(deskController));

    /// <summary>
    /// 一个已经根据 zIndex 排序完成的只读列表
    /// </summary>
    public readonly State<List<DesktopWindowController>> WinList = new(new List<DesktopWindowController>());

    private ChangeableMap<DesktopWindowController, InManageState> AllWindows { get; init; }

    /// <summary>
    /// 当前记录的聚焦窗口
    /// </summary>
    private DesktopWindowController? LastFocusedWin = null;

    /// <summary>
    /// 存储最大化的窗口
    /// </summary>
    private ChangeableSet<DesktopWindowController> HasMaximizedWins = new();

    /// <summary>
    /// 窗口在管理时说需要的一些状态机
    /// </summary>
    /// <param name="DoDestory"></param>
    record InManageState(Action DoDestory);

    public DesktopWindowsManager(DeskController deskController)
	{
		DeskController = deskController;

		AllWindows = new ChangeableMap<DesktopWindowController, InManageState>().Also(it =>
		{
			it.OnChangeAdd(async (wins, _) =>
			{
				var newWinList = wins.Keys.OrderBy(win => win.State.ZIndex).ToList();
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
            });
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




			return win;
		});
	}

	internal void AddNewWindow(DesktopWindowController win)
	{
		_ = Task.Run(() =>
		{

		});
	}

	internal bool RemoveWindow(DesktopWindowController win) =>
        AllWindows.Remove(win)?.Let(inManageState =>
        {
            inManageState.DoDestory();
            return true;
        }) ?? false;

	private void ReOrderZIndex()
	{
		var allWindows = AllWindows.Keys.OrderBy(it => it.State.ZIndex).ToList();
		for (var i = 0; i < allWindows.Count(); i++)
		{
			var win = allWindows[i];
			win.State.ZIndex = i;
		}

		AllWindows.OnChangeEmit();
	}

    /// <summary>
    /// 将指定窗口移动到最上层
    /// </summary>
    private void MoveToTop(DesktopWindowController win)
	{
		/// 窗口被聚焦，那么遍历所有的窗口，为它们重新生成zIndex值
		win.State.ZIndex += AllWindows.Count;
		ReOrderZIndex();
    }

	public Task Focus(DesktopWindowController win) => win.Focus();

	public async Task Focus(Mmid mmid)
	{
		var windows = FindWindows(mmid);
		foreach (var win in windows)
		{
			await win.Focus();
		}
	}

	private List<DesktopWindowController> FindWindows(Mmid mmid)
		=> AllWindows.Keys.ToList().FindAll(win => win.State.Owner == mmid).OrderBy(it => it.State.ZIndex).ToList();

    /// <summary>
    /// 返回最终 isMaximized 的值
    /// </summary>
    /// <param name="mmid"></param>
    /// <returns></returns>
    public async Task<bool> ToggleMaximize(Mmid mmid)
	{
		var windows = FindWindows(mmid);

		/// 只要有一个窗口处于最大化的状态，就当作所有窗口都处于最大化
		var isMaximize = windows.All(win => win.IsMaximized());
		foreach (var win in windows)
		{
			if (isMaximize)
			{
				await win.UnMaximize();
			}
			else
			{
				await win.Maximize();
			}
		}

		return !isMaximize;
    }

}

