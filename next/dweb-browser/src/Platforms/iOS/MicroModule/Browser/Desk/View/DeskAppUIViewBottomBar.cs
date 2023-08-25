using CoreGraphics;
using UIKit;

namespace DwebBrowser.MicroService.Browser.Desk;

public class DeskAppUIViewBottomBar : UIView
{
    public DeskAppUIViewBottomBar()
    {
        var textView = new UITextView();
        textView.Text = "bottombar";
        textView.Frame = Frame;
        textView.TextColor = UIColor.White;

        BackgroundColor = UIColor.Blue;

        AddSubview(textView);

        //var dragGestrue = new UIPanGestureRecognizer(OnDrag);
        //AddGestureRecognizer(dragGestrue);
    }

    //private void OnDrag(UIPanGestureRecognizer pan)
    //{
    //    CGRect originalParentFrame = Superview.Frame;

    //    if (pan.State == UIGestureRecognizerState.Changed)
    //    {
    //        CGPoint dragPoint = pan.TranslationInView(this);

    //        if (Math.Abs(dragPoint.X) < 5 || Math.Abs(dragPoint.Y) < 5)
    //        {
    //            return;
    //        }

    //        CGRect newParentFrame = originalParentFrame;
    //        newParentFrame.Size += new CGSize(dragPoint.X, dragPoint.Y);

    //        Superview.Frame = newParentFrame;

    //        Frame = new CGRect(Frame.X + dragPoint.X,
    //                           Frame.Y + dragPoint.Y,
    //                           Frame.Size.Width,
    //                           Frame.Size.Height);
    //    }
    //    else if (pan.State == UIGestureRecognizerState.Ended)
    //    {
    //        Frame = new CGRect(Superview.Frame.Location, Frame.Size);
    //    }
    //}
}
