using CoreGraphics;
using UIKit;

namespace DwebBrowser.MicroService.Browser.Desk;

public class TaskBarHitView : UIView
{
    /// <summary>
    /// 重写HitTest方法，屏蔽UIView内部点击事件，仅有TaskBarFloatView触发
    /// </summary>
    /// <param name="point"></param>
    /// <param name="uievent"></param>
    /// <returns></returns>
    public override UIView HitTest(CGPoint point, UIEvent uievent)
    {
        if (point.X >= 0 && point.X <= Frame.GetMaxX() && point.Y >= 0 && point.Y <= Frame.GetMaxY())
        {
            return this;
        }

        return base.HitTest(point, uievent);
    }
}

public partial class DeskController
{
    private TaskBarHitView TaskbarFloatView { get; set; }

    public async Task CreateTaskbarFloatView()
    {
        TaskbarFloatView = new()
        {
            Tag = 32767,
            Layer = {
                ZPosition = nfloat.MaxValue
            }
        };

        // 点击
        var tapGesture = new UITapGestureRecognizer(OnFloatTap);
        TaskbarFloatView.AddGestureRecognizer(tapGesture);
        // 拖拽
        var panGesture = new UIPanGestureRecognizer(OnFloatPan);
        TaskbarFloatView.AddGestureRecognizer(panGesture);

        ShowFloatView();
    }

    public void ShowFloatView()
    {
        //DeskUIView.AddSubview(TaskbarFloatView);
        //ResizeTaskbarFloatView();
        //TaskbarFloatView.AddSubview(TaskBarWebView);
        //TaskBarWebView.AutoResize("TaskBarWebView", TaskbarFloatView);
    }

    private void OnFloatTap(UITapGestureRecognizer tap)
    {
        if (tap.State == UIGestureRecognizerState.Ended)
        {
            TaskBarFocusState.Set(true);
        }
    }

    private void OnFloatPan(UIPanGestureRecognizer pan)
    {
        // 计算位移量
        CGPoint translation = pan.TranslationInView(TaskbarFloatView);

        var bounds = UIScreen.MainScreen.Bounds;
        var screenWidth = bounds.Width;
        var screenHeight = bounds.Height;
        var floatHalfWidth = TaskbarFloatView.Center.X - TaskbarFloatView.Frame.X;
        var floatHalfHeight = TaskbarFloatView.Center.Y - TaskbarFloatView.Frame.Y;

        if (pan.State == UIGestureRecognizerState.Changed)
        {
            var point = new CGPoint(TaskbarFloatView.Center.X + translation.X,
                                      TaskbarFloatView.Center.Y + translation.Y);

            // 限制上下左右的可活动区域
            var x = Math.Max(floatHalfWidth + 5, Math.Min(point.X, screenWidth - floatHalfWidth - 5));
            var y = Math.Max(floatHalfHeight + 60, Math.Min(point.Y, screenHeight - floatHalfHeight - 40));
            TaskbarFloatView.Center = new CGPoint(x, y);

            pan.SetTranslation(CGPoint.Empty, TaskbarFloatView);
        }
        else if (pan.State == UIGestureRecognizerState.Ended)
        {
            if (TaskbarFloatView.Center.X < screenWidth / 2)
            {
                UIView.Animate(0.5, () =>
                {
                    TaskbarFloatView.Center = new CGPoint(floatHalfWidth + 5,
                                          TaskbarFloatView.Center.Y);
                });
            }
            else
            {
                UIView.Animate(0.5, () =>
                {
                    TaskbarFloatView.Center = new CGPoint(screenWidth - floatHalfWidth - 5,
                                          TaskbarFloatView.Center.Y);
                });
            }

            // 记录上一次位置
            RecordTaskbarFloatViewFrame = TaskbarFloatView.Frame;
        }
    }

    private CGRect RecordTaskbarFloatViewFrame = CGRect.Empty;

    public void ResizeTaskbarFloatView(int width = 72, int height = 72)
    {
        var screenWidth = UIScreen.MainScreen.Bounds.Width;

        if (RecordTaskbarFloatViewFrame == CGRect.Empty)
        {
            RecordTaskbarFloatViewFrame = new CGRect(screenWidth - width - 5, 200, width, height);
        }
        else
        {
            RecordTaskbarFloatViewFrame = new CGRect(RecordTaskbarFloatViewFrame.X, RecordTaskbarFloatViewFrame.Y, width, height);
        }

        TaskbarFloatView.Frame = RecordTaskbarFloatViewFrame;
    }
}

