using UIKit;

namespace DwebBrowser.MicroService.Browser.Desk;

public class DesktopWindowController : WindowController
{
    public override UIViewController Controller { get; init; }
    internal WindowState State { get; init; }

    public DesktopWindowController(UIViewController controller, WindowState state)
    {
        Controller = controller;
        State = state;
    }

    public override WindowState ToJson() => State;

    public UUID Id => State.Wid;

    protected readonly HashSet<Signal> BlurSignal = new();
    public event Signal OnBlur
    {
        add { if (value != null) lock (BlurSignal) { BlurSignal.Add(value); } }
        remove { lock (BlurSignal) { BlurSignal.Remove(value); } }
    }

    protected readonly HashSet<Signal> FocusSignal = new();
    public event Signal OnFocus
    {
        add { if (value != null) lock (FocusSignal) { FocusSignal.Add(value); } }
        remove { lock (FocusSignal) { FocusSignal.Remove(value); } }
    }

    public bool IsFocused() => State.Focus;

    public async Task Focus()
    {
        if (!State.Focus)
        {
            State.Focus = true;
            await State.EmitChange();
            await FocusSignal.Emit();
        }
    }

    public async Task Blur()
    {
        if (State.Focus)
        {
            State.Focus = false;
            await State.EmitChange();
            await BlurSignal.Emit();
        }
    }

    public bool IsMaximized() => State.Maximize;

    protected readonly HashSet<Signal> MaximizeSignal = new();
    public event Signal OnMaximize
    {
        add { if (value != null) lock (MaximizeSignal) { MaximizeSignal.Add(value); } }
        remove { lock (MaximizeSignal) { MaximizeSignal.Remove(value); } }
    }

    public async Task Maximize()
    {
        if (!State.Maximize)
        {
            State.Maximize = true;
            State.Fullscreen = false;
            State.Minimize = false;
            BeforeMaximizeBounds = State.Bounds;
            await State.EmitChange();
            await MaximizeSignal.Emit();
        }
    }

    private WindowState.WindowBounds? BeforeMaximizeBounds = null;

    /// <summary>
    /// 当窗口从最大化状态退出时触发
    /// Emitted when the window exits from a maximized state.
    /// </summary>
    protected readonly HashSet<Signal> UnMaximizeSignal = new();
    public event Signal OnUnMaximize
    {
        add { if (value != null) lock (UnMaximizeSignal) { UnMaximizeSignal.Add(value); } }
        remove { lock (UnMaximizeSignal) { UnMaximizeSignal.Remove(value); } }
    }

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
            await UnMaximizeSignal.Emit();
        }
    }

    protected readonly HashSet<Signal> MinimizeSignal = new();
    public event Signal OnMinimize
    {
        add { if (value != null) lock (MinimizeSignal) { MinimizeSignal.Add(value); } }
        remove { lock (MinimizeSignal) { MinimizeSignal.Remove(value); } }
    }

    public async Task Minimize()
    {
        if (!State.Minimize)
        {
            State.Minimize = true;
            State.Maximize = false;
            State.Fullscreen = false;
            await State.EmitChange();
            await MinimizeSignal.Emit();
        }
    }
}

