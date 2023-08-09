using UIKit;

namespace DwebBrowser.MicroService.Browser.Desk;

public class DesktopWindowController : WindowController
{
    public override DeskAppController Controller { get; init; }
    internal WindowState State { get; init; }

    public DesktopWindowController(DeskAppController controller, WindowState state)
    {
        Controller = controller;
        State = state;
    }

    public override WindowState ToJson() => State;

    public UUID Id => State.Wid;

    public Listener OnBlur = new();

    public Listener OnFocus = new();

    public bool IsFocused() => State.Focus;

    public async Task Focus()
    {
        if (!State.Focus)
        {
            State.Focus = true;
            await State.EmitChange();
            await OnFocus.Emit();
        }
    }

    public async Task Blur()
    {
        if (State.Focus)
        {
            State.Focus = false;
            await State.EmitChange();
            await OnBlur.Emit();
        }
    }

    public bool IsMaximized() => State.Maximize;

    public Listener OnMaximize = new();

    public async Task Maximize()
    {
        if (!State.Maximize)
        {
            State.Maximize = true;
            State.Fullscreen = false;
            State.Minimize = false;
            BeforeMaximizeBounds = State.Bounds;
            await State.EmitChange();
            await OnMaximize.Emit();
        }
    }

    private WindowState.WindowBounds? BeforeMaximizeBounds = null;

    /// <summary>
    /// 当窗口从最大化状态退出时触发
    /// Emitted when the window exits from a maximized state.
    /// </summary>
    public Listener OnUnMaximize = new();

    /// <summary>
    /// 取消窗口最大化
    /// </summary>
    /// <returns></returns>
    public async Task UnMaximize()
    {
        if (State.Maximize)
        {
            var value = BeforeMaximizeBounds;
            switch (value)
            {
                case null:
                    State.Bounds.Width /= 2;
                    State.Bounds.Left = State.Bounds.Width / 2;
                    State.Bounds.Height /= 2;
                    State.Bounds.Top = State.Bounds.Height / 2;

                    break;
                default:
                    State.Bounds = value;
                    BeforeMaximizeBounds = null;
                    break;
            }

            State.Maximize = false;
            await State.EmitChange();
            await OnUnMaximize.Emit();
        }
    }

    public Listener OnMinimize = new();

    public async Task Minimize()
    {
        if (!State.Minimize)
        {
            State.Minimize = true;
            State.Maximize = false;
            State.Fullscreen = false;
            await State.EmitChange();
            await OnMinimize.Emit();
        }
    }
}

