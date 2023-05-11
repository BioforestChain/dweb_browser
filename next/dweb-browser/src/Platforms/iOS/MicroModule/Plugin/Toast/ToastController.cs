using System;
using CoreGraphics;
using UIKit;

namespace DwebBrowser.Platforms.iOS.MicroModule.Plugin.Toast;

public class ToastController
{
    // 定义 Toast 持续时间的枚举类型
    public enum ToastDuration
    {
        SHORT = 1,
        LONG = 3
    }

    // 定义 Toast 位置的枚举类型
    public enum ToastPosition
    {
        TOP,
        CENTER,
        BOTTOM
    }

    public static Task ShowToastAsync(
        string message,
        UIViewController controller,
        ToastDuration duration = ToastDuration.SHORT,
        ToastPosition position = ToastPosition.BOTTOM)
    {
        return MainThread.InvokeOnMainThreadAsync(async () =>
        {
            // 创建一个标签作为 Toast 的内容
            var toastLabel = new UILabel();
            // 设置标签的文本
            toastLabel.Text = message;
            // 设置标签的文本颜色
            toastLabel.TextColor = UIColor.White;
            // 设置标签的背景颜色和透明度
            toastLabel.BackgroundColor = UIColor.Black.ColorWithAlpha(0.8f);
            // 设置标签的文本对齐方式
            toastLabel.TextAlignment = UITextAlignment.Center;
            // 设置标签的圆角半径
            toastLabel.Layer.CornerRadius = 10;
            // 设置标签的边界裁剪
            toastLabel.Layer.MasksToBounds = true;

            // 计算标签的宽度，留出 20 像素的边距
            var width = controller.View.Frame.Width - 40;
            // 计算标签的高度，根据文本内容和留出 10 像素的上下边距
            var height = toastLabel.SizeThatFits(new CGSize(width, nfloat.MaxValue)).Height + 20;
            // 计算标签的水平坐标，居中显示
            var x = (controller.View.Frame.Width - width) / 2;
            // 计算标签的垂直坐标，距离底部 20 像素
            var y = controller.View.Frame.Height - height - 20;

            // 根据位置参数调整垂直坐标
            switch (position)
            {
                case ToastPosition.TOP:
                    // 顶部位置，距离顶部布局指南 20 像素
                    y = controller.TopLayoutGuide.Length + 20;
                    break;
                case ToastPosition.CENTER:
                    // 中间位置，居中显示
                    y = (controller.View.Frame.Height - height) / 2;
                    break;
                case ToastPosition.BOTTOM:
                    // 底部位置，距离底部布局指南 20 像素
                    y = controller.View.Frame.Height - controller.BottomLayoutGuide.Length - height - 20;
                    break;
            }

            // 设置标签的框架
            toastLabel.Frame = new CGRect(x, y, width, height);

            // 将标签添加到控制器的视图中
            controller.View.AddSubview(toastLabel);

            // 使用动画效果让标签在一定时间后淡出并移除，并在完成后执行回调函数
            UIView.AnimateNotify(
                duration: (double)duration,
                delay: (double)duration,
                options: UIViewAnimationOptions.CurveEaseOut,
                animation: () => { toastLabel.Alpha = 0; },
                completion: (finished) => { if (finished) { toastLabel.RemoveFromSuperview(); } });


            //uIAlertController.Message = message;

            //controller.PresentViewController(uIAlertController, true, null);

            //await Task.Delay(((int)((double)duration * 1000)));
            //uIAlertController.DismissViewController(true, null);
        });
    }
}
