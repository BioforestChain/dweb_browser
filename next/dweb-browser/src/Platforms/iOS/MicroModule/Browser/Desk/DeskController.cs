using DwebBrowser.Base;
using UIKit;
using DwebBrowserFramework;

namespace DwebBrowser.MicroService.Browser.Desk;

public partial class DeskController : BaseViewController
{
    static readonly Debugger Console = new("DeskController");
    public HttpDwebServer TaskbarServer { get; set; }
    public HttpDwebServer DesktopServer { get; set; }

    public DeskNMM DeskNMM { get; init; }

    public DeskController(DeskNMM deskNMM)
    {
        DeskNMM = deskNMM;
    }

    /// <summary>
    /// 用于承载所有App的View
    /// </summary>
    private readonly DeskUIView DeskUIView = new();

    public List<DeskAppUIView> GetDeskAppUIViews()
    {
        var deskAppUIViews = new List<DeskAppUIView>();

        foreach (var view in DeskUIView.Subviews)
        {
            if (view is DeskAppUIView deskAppUIView)
            {
                deskAppUIViews.Add(deskAppUIView);
            }
        }

        return deskAppUIViews;
    }



    public override void ViewDidLoad()
    {
        base.ViewDidLoad();
        View.AddSubview(DeskUIView);
        View.Frame = UIScreen.MainScreen.Bounds;
        DeskUIView.Frame = View.Frame;
    }

    public void InsertSubviewBelow(UIView view, UIView? belowView = null)
    {
        TaskBarFocusState.Set(false);
        DeskUIView.InsertSubviewBelow(view, belowView ?? TaskbarFloatView);
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
        DeskUIView.AddSubview(DesktopView);

        await CreateTaskBarWebView();
        //await CreateTaskbarFloatView();
        CreateTaskBarView();
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
            foreach (var view in DeskUIView.Subviews)
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
            foreach (var view in DeskUIView.Subviews)
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

