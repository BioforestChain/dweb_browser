using CoreGraphics;
using UIKit;

namespace DwebBrowser.MicroService.Browser.Desk;

public class DeskAppUIViewTopBar : UIView
{
    private WindowController Win { get; init; }

	public DeskAppUIViewTopBar(WindowController win)
	{
        Win = win;

		BackgroundColor = UIColor.Red;

        InitMaximizeButtonView();

        #region 手势处理
        var panGesture = new UIPanGestureRecognizer(OnPan);
        AddGestureRecognizer(panGesture);
        var tapGesture = new UITapGestureRecognizer(OnTap)
        {
            // 单击
            NumberOfTapsRequired = 1
        };
        AddGestureRecognizer(tapGesture);
        var longPressGesture = new UILongPressGestureRecognizer(OnLongPress);
        AddGestureRecognizer(longPressGesture);
        #endregion
    }

    #region 手势处理

    private void OnPan(UIPanGestureRecognizer pan)
    {
        // 计算位移量
        CGPoint translation = pan.TranslationInView(Superview);

        var bounds = UIScreen.MainScreen.Bounds;
        var screenWidth = bounds.Width;
        var screenHeight = bounds.Height;
        var floatHalfHeight = Superview.Center.Y - Superview.Frame.Y;

        if (pan.State == UIGestureRecognizerState.Changed)
        {
            var point = new CGPoint(Superview.Center.X + translation.X, Superview.Center.Y + translation.Y);

            // 限制上下左右的可活动区域
            var x = Math.Max(0, Math.Min(point.X, screenWidth));
            var y = Math.Max(floatHalfHeight + 60, Math.Min(point.Y, screenHeight + floatHalfHeight - 60));
            Superview.Center = new CGPoint(x, y);

            pan.SetTranslation(CGPoint.Empty, Superview);
        }
        else if (pan.State == UIGestureRecognizerState.Began)
        {
            Win.InMove.Set(true);
            Win.EmitFocusOrBlur(true);
        }
        else if (pan.State is UIGestureRecognizerState.Ended or UIGestureRecognizerState.Cancelled or UIGestureRecognizerState.Failed)
        {
            Win.InMove.Set(false);
        }
    }

    private void OnTap(UITapGestureRecognizer tap)
    {
        if (tap.State is UIGestureRecognizerState.Ended)
        {
            Win.InMove.Set(false);
            Win.EmitFocusOrBlur(true);
        }
    }

    private void OnLongPress(UILongPressGestureRecognizer longPress)
    {
        if (longPress.State is UIGestureRecognizerState.Began)
        {
            Win.InMove.Set(false);
            Win.EmitFocusOrBlur(true);
        }
    }

    #endregion

    private void InitMaximizeButtonView()
    {
        var view = new TaskBarHitView()
        {
            Frame = new CGRect(10, 10, 20, 20)
        };
        AddSubview(view);

        var textView = new UITextView();
        view.AddSubview(textView);
        textView.Text = "X";
        textView.AutoResize("textView", view);

        var tapGesture = new UITapGestureRecognizer(OnMaximizeTap)
        {
            NumberOfTapsRequired = 1
        };
        view.AddGestureRecognizer(tapGesture);
    }

    private void OnMaximizeTap(UITapGestureRecognizer tap)
    {
        if (tap.State is UIGestureRecognizerState.Ended)
        {
            _ = Win.Maximize().NoThrow();
        }
    }
}

