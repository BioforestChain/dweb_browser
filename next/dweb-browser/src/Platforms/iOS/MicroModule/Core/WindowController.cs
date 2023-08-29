using DwebBrowser.MicroService.Browser.Desk;

namespace DwebBrowser.MicroService.Core;

public partial class WindowController
{
    static readonly Debugger Console = new("WindowController");
    public WindowState State { get; init; }
    public UUID Id { get; init; }

    public WindowController(WindowState state)
    {
        State = state;
        Id = State.Wid;

        OnFocus = CreateStateListener(
            WindowPropertyKeys.Focus,
            (change) => State.Focus,
            (change) => { Console.Log("OnFocus", "emit OnFocus {0}", Id); return unit; });
        OnBlur = CreateStateListener(
            WindowPropertyKeys.Focus,
            (change) => !State.Focus,
            (change) => { Console.Log("OnBlur", "emit OnBlur {0}", Id); return unit; });
        OnModeChange = CreateStateListener(
            WindowPropertyKeys.Mode,
            null,
            (change) => { Console.Log("OnModeChange", "emit OnModeChange {0}", Id); return unit; });
        OnMaximize = CreateStateListener(
            WindowPropertyKeys.Mode,
            (change) => IsMaximized(State.Mode),
            (change) => { Console.Log("OnMaximize", "emit OnMaximize {0}", Id); return unit; });
        OnUnMaximize = CreateStateListener(
            WindowPropertyKeys.Mode,
            (change) => !IsMaximized(State.Mode) && IsMaximized(change.OldValue),
            (change) => { Console.Log("OnUnMaximize", "emit OnUnMaximize {0}", Id); return unit; });
        OnMinimize = CreateStateListener(
            WindowPropertyKeys.Mode,
            (change) => State.Mode == WindowMode.MINIMIZE,
            (change) => { Console.Log("OnMinimize", "emit OnMinimize {0}", Id); return unit; });
        OnClose = CreateStateListener(
            WindowPropertyKeys.Mode,
            (change) => State.Mode == WindowMode.CLOSED,
            (change) => { Console.Log("OnClose", "emit OnClose {0}", Id); return unit; });

    }

    public virtual DesktopWindowsManager DesktopWindowsManager { get; init; }

    public WindowState ToJsonAble() => State;

    #region WindowMode相关的控制函数
    private Listener<dynamic> CreateStateListener(
        WindowPropertyKeys key,
        Func<Observable.Change, bool>? filter,
        Func<Observable.Change, dynamic> map)
    => State.Observable.Listener.CreateChild((change) => change.Key == key.FieldName && filter?.Invoke(change) != false, map);

    

    public Listener<dynamic> OnFocus { get; init; }

    public Listener<dynamic> OnBlur { get; init; }

    public bool IsFocused() => State.Focus;

    public async Task Focus()
    {
        State.Focus = true;
        /// 如果窗口聚焦，那么要同时取消最小化的状态
        await UnMinimize();
    }

    public async Task Blur()
    {
        State.Focus = false;
    }

    public Listener<dynamic> OnModeChange { get; init; }

    public bool IsMaximized(WindowMode? mode = null)
    {
        mode ??= State.Mode;
        return mode == WindowMode.MAXIMIZE || mode == WindowMode.FULLSCREEN;
    }

    public Listener<dynamic> OnMaximize { get; init; }

    public async Task Maximize()
    {
        if (!IsMaximized())
        {
            BeforeMaximizeBounds = State.Bounds with { };
            State.Mode = WindowMode.MAXIMIZE;
        }
    }

    /// <summary>
    /// 记忆窗口最大化之前的记忆
    /// </summary>
    private WindowBounds? BeforeMaximizeBounds = null;

    /// <summary>
    /// 当窗口从最大化状态退出时触发
    /// Emitted when the window exits from a maximized state.
    /// </summary>
    public Listener<dynamic> OnUnMaximize { get; init; }

    public async Task UnMaximize()
    {
        if (IsMaximized())
        {
            var value = BeforeMaximizeBounds;
            switch (value)
            {
                case null:
                    var bounds = new WindowBounds(
                        State.Bounds.width / 4,
                        State.Bounds.height / 4,
                        State.Bounds.width / 2,
                        State.Bounds.height / 2);
                    State.Bounds = bounds with { };

                    break;
                default:
                    State.Bounds = value;
                    BeforeMaximizeBounds = null;
                    break;
            }
            State.Mode = WindowMode.FLOATING;
        }
    }

    public bool IsMinimize(WindowMode? mode = null)
    {
        mode ??= State.Mode;
        return mode == WindowMode.MINIMIZE;
    }

    private WindowMode? BeforeMinimizeMode = null;

    public Listener<dynamic> OnMinimize { get; init; }

    public async Task UnMinimize()
    {
        if (IsMinimize())
        {
            State.Mode = BeforeMinimizeMode ?? WindowMode.FLOATING;
            BeforeMinimizeMode = null;
        }
    }

    public async Task Minimize()
    {
        State.Mode = WindowMode.MINIMIZE;
    }

    public Listener<dynamic> OnClose { get; init; }

    public bool IsClosed() => State.Mode == WindowMode.CLOSED;

    public async Task Close(bool force = false)
    {
        /// 这里的 force 暂时没有作用，未来会加入交互，来阻止窗口关闭
        State.Mode = WindowMode.CLOSED;
    }

    #endregion

    #region 窗口样式修饰

    public async Task SetTopBarStyle(string? contentColor = null, string? backgroundColor = null, bool? overlay = null)
    {
        contentColor?.Also(it => State.TopBarContentColor = it);
        backgroundColor?.Also(it => State.TopBarBackgroundColor = it);
        overlay?.Also(it => State.TopBarOverlay = it);
    }

    public async Task SetBottomBarStyle(
        string? contentColor = null,
        string? backgroundColor = null,
        bool? overlay = null,
        string? theme = null)
    {
        theme?.Also(it => State.BottomBarTheme = WindowBottomBarTheme.From(it));
        contentColor?.Also(it => State.BottomBarContentColor = it);
        backgroundColor?.Also(it => State.BottomBarBackgroundColor = it);
        overlay?.Also(it => State.BottomBarOverlay = it);
    }

    #endregion
}
