﻿using CoreGraphics;
using DwebBrowser.Base;
using System.Drawing;
using CoreGraphics;
using UIKit;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Platform;
using DwebBrowser.MicroService.Sys.Share;
using DwebBrowser.MicroService.Sys.Http;

namespace DwebBrowser.MicroService.Browser.Desk;

public partial class DeskController : BaseViewController
{
    static readonly Debugger Console = new("DeskController");
    public DeskNMM DeskNMM { get; init; }

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

    public async Task Create(HttpDwebServer taskbarServer, HttpDwebServer desktopServer)
    {
        var bounds = UIScreen.MainScreen.Bounds;
        var desktopInternalUrl = desktopServer.StartResult.urlInfo.BuildInternalUrl();
        DesktopView = new DWebView.DWebView(localeMM: DeskNMM, options: new DWebView.DWebView.Options(desktopInternalUrl) { AllowDwebScheme = false })
        {
            Frame = bounds,
            Tag = 32766
        };
        var desktopUrl = desktopInternalUrl.Path("/desktop.html");
        _ = DesktopView.LoadURL(desktopUrl).NoThrow();
        View.AddSubviews(DesktopView);


        TaskBarView = new UIView()
        {
            Frame = new CGRect(bounds.GetMaxX() - 72, (bounds.GetMaxY() - 60) / 2, 60, 60),
            Tag = 32767,
        };
        {
            TaskBarView.ClipsToBounds = true;
            TaskBarView.Layer.CornerRadius = 20f;
            View.AddSubview(TaskBarView);
            /// 背景层
            {
                var effect = UIBlurEffect.FromStyle(UIBlurEffectStyle.Dark);
                var backgroundView = new UIVisualEffectView(effect);
                backgroundView.ContentView.BackgroundColor = UIColor.FromWhiteAlpha(0.7f, 0.3f);
                TaskBarView.AddSubviews(backgroundView);

                /// 布局伸缩到父级
                autoResize("backgroundView", backgroundView);
            }

            /// 内容层
            {
                var taskbarInternalUrl = taskbarServer.StartResult.urlInfo.BuildInternalUrl();
                var contentView = new DWebView.DWebView(localeMM: DeskNMM, options: new DWebView.DWebView.Options(taskbarInternalUrl))
                {
                    Opaque = false,
                    BackgroundColor = UIColor.Clear,
                };
                contentView.ScrollView.BackgroundColor = UIColor.Clear;
                //taskbarServer.StartResult.urlInfo.BuildInternalUrl();
                var taskbarUrl = taskbarInternalUrl.Path("taskbar.html");
                _ = contentView.LoadURL(taskbarUrl).NoThrow();
                TaskBarView.AddSubview(contentView);

                /// 布局伸缩到父级
                autoResize("contentView", contentView);
            }
        }
    }

    /// <summary>
    /// 视图自动伸缩到父级
    /// </summary>
    /// <param name="viewName"></param>
    /// <param name="subView"></param>
    /// <param name="superView"></param>
    private void autoResize(string viewName, UIView subView, UIView? superView = null)
    {
        superView ??= subView.Superview!;

        subView.TranslatesAutoresizingMaskIntoConstraints = false;

        /// 布局伸缩到父级
        var constraints = NSLayoutConstraint.FromVisualFormat($"|[{viewName}]|",
        NSLayoutFormatOptions.DirectionLeadingToTrailing,
        viewName, subView);
        superView.AddConstraints(constraints);

        constraints = NSLayoutConstraint.FromVisualFormat($"V:|[{viewName}]|",
            NSLayoutFormatOptions.DirectionLeadingToTrailing,
            viewName, subView);
        superView.AddConstraints(constraints);
    }

    private bool IsOnTop = false;
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

