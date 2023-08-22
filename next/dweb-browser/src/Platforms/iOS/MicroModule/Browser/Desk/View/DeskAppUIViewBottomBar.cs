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
    }
}

