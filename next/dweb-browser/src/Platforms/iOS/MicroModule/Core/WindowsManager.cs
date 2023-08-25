using DwebBrowser.Base;
using DwebBrowser.MicroService.Browser.Desk;

namespace DwebBrowser.MicroService.Core;

/// <summary>
/// 窗口在管理时说需要的一些状态机
/// </summary>
/// <param name="DoDestroy"></param>
internal record InManagerState(Action DoDestroy);

public class WindowsManager
{
    internal virtual BaseViewController Controller { get; init; }

    public WindowsManager(BaseViewController controller)
    {
        Controller = controller;
    }

    /// <summary>
    /// 一个已经根据 zIndex 排序完成的只读列表
    /// </summary>
    public readonly State<List<DesktopWindowController>> WinList = new(new List<DesktopWindowController>());

    /// <summary>
    /// 置顶窗口，一个已经根据 zIndex 排序完成的只读列表
    /// </summary>
    public readonly State<List<DesktopWindowController>> WinListTop = new(new List<DesktopWindowController>());

    public ChangeableMap<DesktopWindowController, InManageState> AllWindows { get; init; } = new();

    /// <summary>
    /// 当前记录的聚焦窗口
    /// </summary>
    private DesktopWindowController? LastFocusedWin
    {
        get
        {
            /// 从最顶层的窗口往下遍历
            DesktopWindowController? findInWinList(List<DesktopWindowController> winList)
            {
                winList.Reverse();
                foreach (var win in winList)
                {
                    if (win.IsFocused())
                    {
                        /// 如果发现之前赋值过，这时候需要将之前的窗口给blur掉
                        return win;
                    }
                }

                return null;
            }

            return findInWinList(WinList.Get()) ?? findInWinList(WinListTop.Get());
        }
    }

    /// <summary>
    /// 确保窗口现在只对最后一个元素聚焦
    ///
    /// 允许不存在聚焦的窗口，聚焦应该由用户行为触发
    /// </summary>
    /// <returns></returns>
    internal async Task<DesktopWindowController?> DoLastFocusedWin()
    {
        DesktopWindowController? lastFocusedWin = null;

        /// 从最底层的窗口往上遍历
        async Task findInWinList(List<DesktopWindowController> winList)
        {
            foreach (var win in winList)
            {
                if (win.IsFocused())
                {
                    /// 如果发现之前赋值过，这时候需要将之前的窗口给blur掉
                    lastFocusedWin?.Blur();
                    lastFocusedWin = win;
                }
            }
        }

        await findInWinList(WinList.Get());
        await findInWinList(WinListTop.Get());
        return lastFocusedWin;
    }

    /// <summary>
    /// 存储最大化的窗口
    /// </summary>
    private readonly ChangeableSet<DesktopWindowController> HasMaximizedWins = new();

    /// <summary>
    /// 窗口在管理时说需要的一些状态机
    /// </summary>
    /// <param name="DoDestory"></param>
    public record InManageState(Action DoDestory);


    internal void AddNewWindow(DesktopWindowController win, bool autoFocus = true)
    {
        /// 对窗口做一些启动准备
        var offListenerList = new List<Action>();

        win.OnFocus.OnListener += async (_, self) =>
        {
            FocusWindow(win);
            offListenerList.Add(() => win.OnFocus.OnListener -= self);
        };

        win.OnMaximize.OnListener += async (_, self) =>
        {
            HasMaximizedWins.Add(win);
            offListenerList.Add(() => win.OnMaximize.OnListener -= self);
        };

        win.OnUnMaximize.OnListener += async (_, self) =>
        {
            HasMaximizedWins.Remove(win);
            offListenerList.Add(() => win.OnUnMaximize.OnListener -= self);
        };

        /// 立即执行
        if (win.IsMaximized())
        {
            HasMaximizedWins.Add(win);
        }

        /// 窗口销毁的时候，做引用释放
        win.OnClose.OnListener += async (_, self) =>
        {
            RemoveWindow(win);
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
        if (autoFocus)
        {
            FocusWindow(win);
        }
    }

    internal bool RemoveWindow(DesktopWindowController win, bool autoFocus = true) =>
        AllWindows.Remove(win)?.Let(inManageState =>
        {
            /// 移除最大化窗口集合
            HasMaximizedWins.Remove(win);

            if (autoFocus)
            {
                /// 对窗口进行重新排序
                ReOrderZIndex();
            }

            /// 最后，销毁绑定事件
            inManageState.DoDestory();

            return true;
        }) ?? false;

    private void ReOrderZIndex()
    {
        /// 根据 alwaysOnTop 进行分组
        var winList = new List<DesktopWindowController>();
        var winListTop = new List<DesktopWindowController>();

        foreach (var win in AllWindows.Keys)
        {
            if (win.State.AlwaysOnTop)
            {
                winListTop.Add(win);
            }
            else
            {
                winList.Add(win);
            }
        }

        /// 对窗口的 zIndex 进行重新赋值
        int resetZIndex(List<DesktopWindowController> list, State<List<DesktopWindowController>> state)
        {
            var changes = Math.Abs(list.Count - state.Get().Count);
            var sortedList = list.OrderBy(it => it.State.ZIndex).ToList();

            for (var i = 0; i < sortedList.Count; i++)
            {
                var win = sortedList[i];
                if (win.State.ZIndex != i + 1)
                {
                    win.State.ZIndex = i + 1;
                    changes += 1;
                }
            }

            if (changes > 0)
            {
                state.Set(sortedList);
            }

            return changes;
        }

        var anyChanges = resetZIndex(winList, WinList) + resetZIndex(winListTop, WinListTop);

        if (anyChanges > 0)
        {
            AllWindows.OnChangeEmit();
        }
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

    /// <summary>
    /// 对一个窗口做聚焦操作
    /// </summary>
    /// <param name="win"></param>
    /// <returns></returns>
    public void FocusWindow(DesktopWindowController win)
    {
        _ = Task.Run(async () =>
        {
            var preFocusedWin = LastFocusedWin;
            switch (preFocusedWin == win)
            {
                case true:
                    break;
                default:
                    LastFocusedWin?.Blur();
                    await win.Focus();
                    MoveToTop(win);
                    await DoLastFocusedWin();
                    break;

            }
        }).NoThrow();
    }

    /// <summary>
    /// 对一些窗口做聚焦操作
    /// </summary>
    /// <param name="mmid"></param>
    /// <returns></returns>
    public async Task FocusWindow(Mmid mmid)
    {
        var windows = FindWindows(mmid);

        if (LastFocusedWin is not null && !windows.Contains(LastFocusedWin))
        {
            await LastFocusedWin.Blur();
        }

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

