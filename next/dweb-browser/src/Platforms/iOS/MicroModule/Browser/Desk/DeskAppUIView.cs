using CoreGraphics;
using UIKit;

namespace DwebBrowser.MicroService.Browser.Desk;

public class DeskAppUIView : UIView
{
    static readonly Debugger Console = new("DeskAppUIView");

    public WindowController Win { get; init; }

    public DeskAppUIView(WindowController win)
    {
        Win = win;
        
        /// 设置圆角
        ClipsToBounds = true;
        Layer.CornerRadius = 40f;
        Frame = new CGRect(100, 100, 100, 100);
        BackgroundColor = UIColor.White;
    }

    public void Render(UIView view, WindowRenderScope windowRenderScope)
    {
        var bounds = windowRenderScope.Bounds;
        Frame = new CGRect(bounds.Left, bounds.Top, bounds.Width, bounds.Height);

        InitContentView();

        var topbar = new DeskAppUIViewTopBar(Win) { Frame = new CGRect(0, 0, Frame.Width, 60) };
        topbar.LeadingAnchor.ConstraintEqualTo(LeadingAnchor);
        topbar.TrailingAnchor.ConstraintEqualTo(TrailingAnchor);
        var bottombar = new DeskAppUIViewBottomBar() { Frame = new CGRect(0, Frame.Height - 34, Frame.Width, 34) };

        AddSubviews(topbar, bottombar);

        AddContentView(view);

        DeskNMM.DeskController.InsertSubviewBelow(this);
    }

    public UIView ContentView = new();

    public void InitContentView()
    {
        
        ContentView.ClipsToBounds = true;
        //ContentView.Layer.CornerRadius = 20f;
        ContentView.Frame = new CGRect(0, 40, Frame.Width, Frame.Height - 74);
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

