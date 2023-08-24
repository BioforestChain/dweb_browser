
using CoreGraphics;
using System.Collections.Concurrent;
using UIKit;

namespace DwebBrowser.MicroService.Browser.Desk;

public partial class DeskController
{
    /// <summary>
    /// 页面启动
    /// </summary>
    public void Start()
    {
        //foreach (var view in DeskUIView.Subviews)
        //{
        //    if (view is DeskAppUIView deskAppUIView)
        //    {
        //        deskAppUIView.RemoveFromSuperview();
        //    }
        //}

        Console.Log("Start", "render");
        DesktopWindowsManager.Render();
    }

    private DesktopWindowsManager? PreDesktopWindowsManager = null;

    public DesktopWindowsManager DesktopWindowsManager
    {
        get => DesktopWindowsManager.GetInstance(this, dwm =>
        {
            ///// 但有窗口信号变动的时候，确保 Activity 事件被激活
            dwm.AllWindows.OnChangeAdd(async (_, self) =>
            {
                await OnActivity.Emit();
                dwm.Controller.OnDestroy.OnListener += async (_) =>
                {
                    dwm.AllWindows.OnChangeRemove(self);
                };
            });

            PreDesktopWindowsManager?.Also(preDwm =>
            {
                /// 窗口迁移
                foreach (var win in preDwm.AllWindows.Keys)
                {
                    preDwm.RemoveWindow(win);
                    dwm.AddNewWindow(win);
                }
                PreDesktopWindowsManager = null;
            });

            PreDesktopWindowsManager = dwm;
        });
    }

    private readonly LazyBox<State<CGRect>> LazyCurrentInsets = new();
    public State<CGRect> CurrentInsets => LazyCurrentInsets.GetOrPut(() => new State<CGRect>(UIScreen.MainScreen.Bounds));

    public Listener OnActivity = new();

    public record MainDwebView(string Name, DWebView.DWebView Webview);

    public ConcurrentDictionary<string, MainDwebView> MainDwebViews = new();

    public MainDwebView CreateMainDwebView(string name, string initUrl = "") => MainDwebViews.GetValueOrPut(name, () =>
    {
        var webview = new DWebView.DWebView(DeskNMM, options: new DWebView.DWebView.Options(initUrl));
        return new MainDwebView(name, webview);
    });
}

