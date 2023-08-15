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
        var topView = belowView;

        if (belowView is null)
        {
            if (TaskBarFocusState.Get())
            {
                topView = TaskbarFloatView;
                TaskBarFocusState.Set(false);
            }
            else
            {
                topView = TaskBarView;
            }
        }

        if (topView is not null)
        {
            View.InsertSubviewBelow(view, topView);
        }
        else
        {
            View.AddSubview(view);
        }
    }

    public void InsertSubviewAbove(UIView view, UIView? aboveView = null)
    {
        if (aboveView is not null || DesktopView is not null)
        {
            View.InsertSubviewAbove(view, aboveView ?? DesktopView);
        }
        else
        {
            View.AddSubview(view);
        }
    }

    /// <summary>
    /// 为了将TaskBarView置于顶层，且可交互，必须使用 BringSubviewToFront
    /// </summary>
    /// <param name="view"></param>
    public void BringSubviewToFront(UIView? view = null)
    {
        View.BringSubviewToFront(view ?? TaskBarView);
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

        await StartTaskBarWebView();
        //await CreateTaskBarView();
        await InitTaskbarFloatView();
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

