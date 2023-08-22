using System.Collections.Concurrent;
using CoreGraphics;
using UIKit;

namespace DwebBrowser.MicroService.Browser.Desk;

public class DeskAppUIView : DeskUIView
{
    static readonly Debugger Console = new("DeskAppUIView");
    public DeskAppUIView()
    {
        /// 设置圆角
        ClipsToBounds = true;
        Layer.CornerRadius = 20f;
        Frame = new CGRect(100, 100, 100, 100);
        BackgroundColor = UIColor.White;
    }

    public static void Start()
    {
        Console.Log("Start", "render");
        var deskAppUIView = new DeskAppUIView();
        DeskNMM.DeskController.DesktopWindowsManager.Render(deskAppUIView);
    }

    public void Render(UIView view, WindowRenderScope windowRenderScope)
    {
        Frame = new CGRect(100, 100, windowRenderScope.Width, windowRenderScope.Height);

        InitContentView();

        var topbar = new DeskAppUIViewTopBar() { Frame = new CGRect(0, 0, Frame.Width, 10) };
        var bottombar = new DeskAppUIViewBottomBar() { Frame = new CGRect(0, Frame.Height - 10, Frame.Width, 10) };

        AddSubviews(topbar, bottombar);

        AddContentView(view);

        DeskNMM.DeskController.InsertSubviewBelow(this);
    }

    public UIView ContentView = new();

    public void InitContentView()
    {
        ContentView.ClipsToBounds = true;
        ContentView.Layer.CornerRadius = 20f;
        ContentView.Frame = new CGRect(0, 10, Frame.Width, Frame.Height - 20);
        AddSubview(ContentView);
    }

    public void AddContentView(UIView view)
    {
        ContentView.AddSubview(view);
        view.AutoResize("DeskAppUIView", ContentView);
    }

    private readonly HashSet<Signal> DestroySignal = new();
    public event Signal OnDestroy
    {
        add { if (value != null) lock (DestroySignal) { DestroySignal.Add(value); } }
        remove { lock (DestroySignal) { DestroySignal.Remove(value); } }
    }

    public Task EmitAndClear() => DestroySignal.EmitAndClear();
}

