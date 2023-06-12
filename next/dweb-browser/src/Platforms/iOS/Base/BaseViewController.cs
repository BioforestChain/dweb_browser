using UIKit;
using CoreGraphics;

#nullable enable

namespace DwebBrowser.Base;

public abstract class BaseViewController : UIViewController
{
    public override void ViewDidLoad()
    {
        base.ViewDidLoad();
        InitData();
    }

    /// <summary>
    /// 初始化数据，或者注册监听
    /// </summary>
    public virtual void InitData() { }


    public virtual void InitView() { }

    public event Signal? OnDestroyController;

    public override void ViewWillDisappear(bool animated)
    {
        base.ViewWillDisappear(animated);
        _ = Task.Run(async () =>
        {
            await (OnDestroyController?.Emit()).ForAwait();
        }).NoThrow();
    }



    protected void AddDebugPoints()
    {

    }
}

public static class UIViewExtendsions
{
    public class DebugPointsView : UIView
    {
        public readonly uint PointSize = 30;
        public readonly UIColor PointColor = UIColor.Red;
        public DebugPointsView(uint pointSize = default, UIColor? pointColor = default)
        {
            if (pointSize is not 0)
            {
                PointSize = pointSize;
            }
            if (pointColor is not null)
            {
                PointColor = pointColor;
            }
            AutoresizingMask = UIViewAutoresizing.FlexibleWidth | UIViewAutoresizing.FlexibleHeight;
        }
        public override void LayoutSubviews()
        {
            base.LayoutSubviews();
            /// 填充父级的宽高
            Frame = new CGRect(0, 0, Superview.Bounds.Width, Superview.Bounds.Height);
            /// 添加子视图
            var pFrame = Frame;
            var pView = this;
            var dSize = PointSize;

            var point1 = new UIView(new CGRect(pFrame.Left, pFrame.Top, dSize, dSize)).Also(it => it.BackgroundColor = UIColor.Red);
            pView.AddSubview(point1);
            var point2 = new UIView(new CGRect(pFrame.Right - dSize, pFrame.Top, dSize, dSize)).Also(it => it.BackgroundColor = UIColor.Blue);
            pView.AddSubview(point2);
            var point3 = new UIView(new CGRect(pFrame.Left, pFrame.Bottom - dSize, dSize, dSize)).Also(it => it.BackgroundColor = UIColor.Green);
            pView.AddSubview(point3);
            var point4 = new UIView(new CGRect(pFrame.Right - dSize, pFrame.Bottom - dSize, dSize, dSize)).Also(it => it.BackgroundColor = UIColor.Purple);
            pView.AddSubview(point4);
        }
    }
    public static void AddDebugPoints(this UIView pView, uint pointSize = default, UIColor? pointColor = default)
    {
        var debugPointsView = new DebugPointsView(pointSize, pointColor);
        //debugPointsView.Frame = pView.Frame;
        pView.AddSubview(debugPointsView);
    }
}