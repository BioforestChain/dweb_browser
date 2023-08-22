using CoreGraphics;
using System.Collections.Concurrent;
using UIKit;

namespace DwebBrowser.MicroService.Browser.Desk;

public class DeskUIView : UIView
{
    public override void WillRemoveSubview(UIView uiview)
    {
        if (uiview is DeskAppUIView deskAppUIView)
        {
            deskAppUIView.EmitAndClear();
        }
    }
}

