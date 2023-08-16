using System.Runtime.InteropServices;
using CoreGraphics;
using UIKit;

namespace DwebBrowser.MicroService.Browser.Desk;

public partial class DeskController : DeskAppController
{
    static readonly Debugger Console = new("DeskController");
    public HttpDwebServer TaskbarServer { get; set; }
    public HttpDwebServer DesktopServer { get; set; }

    public DeskController(DeskNMM deskNMM) : base(deskNMM)
    { }

    public override void ViewDidLoad()
    {
        base.ViewDidLoad();

        View.Frame = UIScreen.MainScreen.Bounds;
        View.BackgroundColor = UIColor.White;
    }

    public void InsertSubviewBelow(UIView view, UIView? belowView = null)
    {
        TaskBarFocusState.Set(false);
        View.InsertSubviewBelow(view, belowView ?? TaskbarFloatView);
    }

    public async Task Create()
    {
        var bounds = UIScreen.MainScreen.Bounds;

        var desktopInternalUrl = GetDesktopUrl().Uri.ToString();
        DesktopView = new DWebView.DWebView(
            localeMM: DeskNMM,
            options: new DWebView.DWebView.Options(desktopInternalUrl) { AllowDwebScheme = false })
        {
            Frame = bounds,
            Tag = 32766
        };
        _ = DesktopView.LoadURL(desktopInternalUrl).NoThrow();
        View.AddSubview(DesktopView);

        await CreateTaskBarWebView();
        await CreateTaskbarFloatView();
    }

    public bool IsOnTop = false;
    /// <summary>
    /// desktop、taskbarfloat tags标识
    /// </summary>
    private readonly nint[] ViewTags = new[] { (nint)32766, 32767 };
    /// <summary>
    /// 将其它视图临时最小化到 TaskbarView/TooggleDesktopButton 按钮里头，在此点击该按钮可以释放这些临时视图到原本的状态
    /// </summary>
    /// <returns></returns>
    public bool ToggleDesktopView()
    {
        TaskBarFocusState.Set(false);
        if (IsOnTop)
        {
            IsOnTop = false;
            foreach (var view in View.Subviews)
            {
                if (!ViewTags.Contains(view.Tag))
                {
                    view.Hidden = false;
                }
            }
        }
        else
        {
            IsOnTop = true;
            foreach (var view in View.Subviews)
            {
                if (!ViewTags.Contains(view.Tag))
                {
                    view.Hidden = true;
                }
            }
        }

        return IsOnTop;
    }
}

