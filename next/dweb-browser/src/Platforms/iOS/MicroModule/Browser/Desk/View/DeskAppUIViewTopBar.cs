using UIKit;

namespace DwebBrowser.MicroService.Browser.Desk;

public class DeskAppUIViewTopBar : UIView
{
	public DeskAppUIViewTopBar()
	{
		var textView = new UITextView();
		textView.Text = "topbar";
		textView.Frame = Frame;
		textView.TextColor = UIColor.White;

		BackgroundColor = UIColor.Red;

		AddSubview(textView);
	}
}

