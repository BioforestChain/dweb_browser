using System.Runtime.CompilerServices;
using DwebBrowser.Helper;
using UIKit;

namespace DwebBrowser.MicroService.Browser.Desk;

public partial class DesktopWindowsManager
{
    static readonly Debugger Console = new("DesktopWindowsManager");
    internal DeskAppController DeskAppController { get; init; }

    private static readonly ConditionalWeakTable<DeskAppController, DesktopWindowsManager> Instances = new();

    public static DesktopWindowsManager GetInstance(DeskAppController deskAppController, Action<DesktopWindowsManager> onPut)
        => Instances.GetValueOrPut(deskAppController, () => {
            return new DesktopWindowsManager(deskAppController).Also(dwm =>
            {
                onPut(dwm);
                deskAppController.OnDestroy.OnListener += async (_) =>
                {
                    Instances.Remove(deskAppController);
                };
            });
        });

    /// <summary>
    /// 一个已经根据 zIndex 排序完成的只读列表
    /// </summary>
    public readonly State<List<DesktopWindowController>> WinList = new(new List<DesktopWindowController>());

    public ChangeableMap<DesktopWindowController, InManageState> AllWindows { get; init; }

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
    public record InManageState(Action DoDestory);

    public DesktopWindowsManager(DeskAppController deskAppController)
    {
        DeskAppController = deskAppController;

        AllWindows = new ChangeableMap<DesktopWindowController, InManageState>().Also(it =>
        {
            it.OnChangeAdd(async (wins, self) =>
            {
                /// 从小到大排序
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

                DeskAppController.OnDestroy.OnListener += async (_) =>
                {
                    it.OnChangeRemove(self);
                };
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

            var win = new DesktopWindowController(DeskAppController, winState);
            AddNewWindow(win);

            return win;
        });

        DeskAppController.OnDestroy.OnListener += async (_) =>
        {
            offAdapter();
        };
    }

    internal void AddNewWindow(DesktopWindowController win)
    {
        /// 对窗口做一些启动准备
        var offListenerList = new List<Action>();

        win.OnFocus.OnListener += async (self) =>
        {
            if (LastFocusedWin != win)
            {
                LastFocusedWin?.Blur();
                LastFocusedWin = win;
                MoveToTop(win);
            }

            offListenerList.Add(() => win.OnFocus.OnListener -= self);
        };

        /// 如果窗口释放聚焦，那么释放引用
        win.OnBlur.OnListener += async (self) =>
        {
            if (LastFocusedWin == win)
            {
                LastFocusedWin = null;
            }

            offListenerList.Add(() => win.OnBlur.OnListener -= self);
        };

        win.OnMaximize.OnListener += async (self) =>
        {
            Console.Log("OnMaximize", "maximized");
            HasMaximizedWins.Add(win);

            offListenerList.Add(() => win.OnMaximize.OnListener -= self);
        };

        win.OnUnMaximize.OnListener += async (self) =>
        {
            Console.Log("OnUnMaximize", "unmaximized");
            HasMaximizedWins.Remove(win);

            offListenerList.Add(() => win.OnUnMaximize.OnListener -= self);
        };

        /// 立即执行
        if (win.IsMaximized())
        {
            HasMaximizedWins.Add(win);
        }

        /// 窗口销毁的时候，做引用释放
        win.OnClose.OnListener += async (self) =>
        {
            RemoveWindow(win);
            offListenerList.Add(() => win.OnClose.OnListener -= self);
        };

        /// 存储窗口与它的 状态机（销毁函数）
        AllWindows[win] = new InManageState(() =>
        {
            foreach (var off in offListenerList)
            {
                off();
            }
        });

        /// 第一次装载窗口，默认将它聚焦到最顶层
        Focus(win);
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
        for (var i = 0; i < allWindows.Count; i++)
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

