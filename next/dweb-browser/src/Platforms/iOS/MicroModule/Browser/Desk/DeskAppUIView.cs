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
        Layer.CornerRadius = 20f;
        Frame = new CGRect(100, 100, 100, 100);
        BackgroundColor = UIColor.White;

        var panGesture = new UIPanGestureRecognizer(OnPan);
        AddGestureRecognizer(panGesture);
    }

    private void OnPan(UIPanGestureRecognizer pan)
    {
        // 计算位移量
        CGPoint translation = pan.TranslationInView(this);

        var bounds = UIScreen.MainScreen.Bounds;
        var screenWidth = bounds.Width;
        var screenHeight = bounds.Height;
        var floatHalfWidth = Center.X - Frame.X;
        var floatHalfHeight = Center.Y - Frame.Y;

        if (pan.State == UIGestureRecognizerState.Changed)
        {
            var point = new CGPoint(Center.X + translation.X, Center.Y + translation.Y);

            // 限制上下左右的可活动区域
            var x = Math.Max(floatHalfWidth + 5, Math.Min(point.X, screenWidth - floatHalfWidth - 5));
            var y = Math.Max(floatHalfHeight + 60, Math.Min(point.Y, screenHeight - floatHalfHeight - 40));
            Center = new CGPoint(x, y);

            pan.SetTranslation(CGPoint.Empty, this);
        }
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

