using UIKit;

namespace DwebBrowser.Helper;

public static class UIViewExtensions
{
    /// <summary>
    /// 视图自动伸缩到父级
    /// </summary>
	public static void AutoResize(this UIView self, string viewName, UIView? superView = null)
	{
        superView ??= self.Superview!;

        self.TranslatesAutoresizingMaskIntoConstraints = false;

        /// 布局伸缩到父级
        var constraints = NSLayoutConstraint.FromVisualFormat($"|[{viewName}]|",
        NSLayoutFormatOptions.DirectionLeadingToTrailing,
        viewName, self);
        superView.AddConstraints(constraints);

        constraints = NSLayoutConstraint.FromVisualFormat($"V:|[{viewName}]|",
            NSLayoutFormatOptions.DirectionLeadingToTrailing,
            viewName, self);
        superView.AddConstraints(constraints);
    }
}

