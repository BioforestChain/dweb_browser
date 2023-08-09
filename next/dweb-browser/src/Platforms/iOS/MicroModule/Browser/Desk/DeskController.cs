using System.Runtime.InteropServices;
using CoreGraphics;
using DwebBrowser.Base;
using UIKit;

namespace DwebBrowser.MicroService.Browser.Desk;

public partial class DeskController : BaseViewController
{
    static readonly Debugger Console = new("DeskController");
    public DeskNMM DeskNMM { get; init; }
    public HttpDwebServer TaskbarServer { get; set; }
    public HttpDwebServer DesktopServer { get; set; }

    public DeskController(DeskNMM deskNMM)
    {
        DeskNMM = deskNMM;
    }

    public override void ViewDidLoad()
    {
        base.ViewDidLoad();

        View.Frame = UIScreen.MainScreen.Bounds;
        View.BackgroundColor = UIColor.White;
    }

    /// <summary>
    /// 为了将TaskBarView置于顶层，且可交互，必须使用 BringSubviewToFront
    /// </summary>
    /// <param name="view"></param>
    public void AddSubView(UIView view)
    {
        View.AddSubview(view);
        View.BringSubviewToFront(TaskBarView);
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

        TaskBarView = new UIView()
        {
            Frame = new CGRect(bounds.GetMaxX() - 72, (bounds.GetMaxY() - 60) / 2, 60, 60),
            Tag = 32767,
        };
        {
            TaskBarView.ClipsToBounds = true;
            TaskBarView.Layer.CornerRadius = 20f;
            TaskBarView.Layer.ZPosition = NFloat.MaxValue;

            View.AddSubview(TaskBarView);
            /// 背景层
            {
                var effect = UIBlurEffect.FromStyle(UIBlurEffectStyle.Dark);
                var backgroundView = new UIVisualEffectView(effect);
                backgroundView.ContentView.BackgroundColor = UIColor.FromWhiteAlpha(0.7f, 0.3f);
                TaskBarView.AddSubviews(backgroundView);

                /// 布局伸缩到父级
                backgroundView.AutoResize("backgroundView");
            }

            /// 内容层
            {
                var taskbarInternalUrl = GetTaskbarUrl().Uri.ToString();
                var contentView = new DWebView.DWebView(
                    localeMM: DeskNMM,
                    options: new DWebView.DWebView.Options(taskbarInternalUrl) { AllowDwebScheme = false })
                {
                    Opaque = false,
                    BackgroundColor = UIColor.Clear,
                };
                contentView.ScrollView.BackgroundColor = UIColor.Clear;

                _ = contentView.LoadURL(taskbarInternalUrl).NoThrow();
                TaskBarView.AddSubview(contentView);

                /// 布局伸缩到父级
                contentView.AutoResize("contentView");
            }
        }
    }

    public bool IsOnTop = false;
    /// <summary>
    /// desktop和taskbar tags标识
    /// </summary>
    private readonly nint[] ViewTags = new[] { (nint)32766, 32767 };
    /// <summary>
    /// 将其它视图临时最小化到 TaskbarView/TooggleDesktopButton 按钮里头，在此点击该按钮可以释放这些临时视图到原本的状态
    /// </summary>
    /// <returns></returns>
    public bool ToggleDesktopView()
    {
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

