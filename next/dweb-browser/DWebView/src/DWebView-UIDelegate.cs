using AVFoundation;
using DwebBrowser.Helper;
using static DwebBrowser.Helper.Prelude;

namespace DwebBrowser.DWebView;

public static class UIViewExtendsions
{
    public static UIViewController? GetUIViewController(this UIView view)
    {
        UIResponder responder = view.NextResponder;
        while (responder is not null)
        {
            if (responder is UIViewController vc)
            {
                return vc;
            }
            responder = responder.NextResponder;
        }
        return null;
    }
}

public partial class DWebView : WKWebView
{
    private readonly HashSet<Signal<(WKWebView webView, WKWebViewConfiguration configuration, WKNavigationAction navigationAction, WKWindowFeatures windowFeatures, Action<WKWebView?> completionHandler)>> CreateWebViewSignal = new();
    public event Signal<(WKWebView webView, WKWebViewConfiguration configuration, WKNavigationAction navigationAction, WKWindowFeatures windowFeatures, Action<WKWebView?> completionHandler)> OnCreateWebView
    {
        add { if (value != null) lock (CreateWebViewSignal) { CreateWebViewSignal.Add(value); } }
        remove { lock (CreateWebViewSignal) { CreateWebViewSignal.Remove(value); } }
    }
    private readonly HashSet<Signal<WKWebView>> CloseSignal = new();
    public event Signal<WKWebView> OnClose
    {
        add { if (value != null) lock (CloseSignal) { CloseSignal.Add(value); } }
        remove { lock (CloseSignal) { CloseSignal.Remove(value); } }
    }

    #region 这些是可以自定义的行为，SignalResult 的 Complete 代表覆盖了默认的行为、Next 代表跳过管控，那么最后就会显示我们提供的默认行为
    private readonly HashSet<Signal<(WKWebView webView, string message, WKFrameInfo frame), SignalResult<Unit>>> JsAlertSignal = new();
    public event Signal<(WKWebView webView, string message, WKFrameInfo frame), SignalResult<Unit>> OnJsAlert
    {
        add { if (value != null) lock (JsAlertSignal) { JsAlertSignal.Add(value); } }
        remove { lock (JsAlertSignal) { JsAlertSignal.Remove(value); } }
    }
    private readonly HashSet<Signal<(WKWebView webView, string message, WKFrameInfo frame), SignalResult<bool>>> JsConfirmSignal = new();
    public event Signal<(WKWebView webView, string message, WKFrameInfo frame), SignalResult<bool>> OnJsConfirm
    {
        add { if (value != null) lock (JsConfirmSignal) { JsConfirmSignal.Add(value); } }
        remove { lock (JsConfirmSignal) { JsConfirmSignal.Remove(value); } }
    }
    private readonly HashSet<Signal<(WKWebView webView, string prompt, string? defaultText, WKFrameInfo frame), SignalResult<string?>>> JsPromptSignal = new();
    public event Signal<(WKWebView webView, string prompt, string? defaultText, WKFrameInfo frame), SignalResult<string?>> OnJsPrompt
    {
        add { if (value != null) lock (JsPromptSignal) { JsPromptSignal.Add(value); } }
        remove { lock (JsPromptSignal) { JsPromptSignal.Remove(value); } }
    }
    #endregion


    public class DWebViewUIDelegate : WKUIDelegate
    {
        readonly DWebView dWebView;
        internal DWebViewUIDelegate(DWebView dWebView)
        {
            this.dWebView = dWebView;
        }

        [Export("webView:createWebViewWithConfiguration:forNavigationAction:windowFeatures:")]
        public override WKWebView? CreateWebView(WKWebView webView, WKWebViewConfiguration configuration, WKNavigationAction navigationAction, WKWindowFeatures windowFeatures)
        {
            WKWebView? result = null;
            var completionHandler = (WKWebView? webView) =>
            {
                result = webView;
            };
            var args = (webView, configuration, navigationAction, windowFeatures, completionHandler);

            var url = navigationAction.Request.Url.AbsoluteString;
            Action CreateAction = () =>
            {
                if (url is not null && dWebView.CloseWatcherController.Consuming.Remove(url))
                {
                    var consumeToken = url!;
                    var watcher = dWebView.CloseWatcherController.Apply(
                        navigationAction.TargetFrame is null || !navigationAction.TargetFrame.MainFrame);
                    dWebView.InvokeOnMainThread(() =>
                    {
                        dWebView.Dispose();
                        dWebView.CloseWatcherController.ResolveToken(consumeToken, watcher);
                    });
                }
                else
                {
                    (dWebView.CreateWebViewSignal.Emit(args))?.Wait();
                }
            };

            if (url is null)
            {
                dWebView.OnReady += async (_) =>
                {
                    url = navigationAction.Request.Url.AbsoluteString;
                    CreateAction();
                };
            }
            else
            {
                CreateAction();
            }

            return result;
        }
        [Export("webViewDidClose:")]
        public override void DidClose(WKWebView webView)
        {
            dWebView.CloseSignal.Emit(webView)?.Wait();
        }


        [Export("webView:runJavaScriptAlertPanelWithMessage:initiatedByFrame:completionHandler:")]
        public override async void RunJavaScriptAlertPanel(WKWebView webView, string message, WKFrameInfo frame, Action completionHandler)
        {
            await dWebView.JsAlertSignal.EmitForResult((webView, message, frame), async (args, ctx, _) =>
            {
                if (webView.GetUIViewController() is not null and var vc)
                {
                    var alertController = UIAlertController.Create(args.webView.Title, args.message, UIAlertControllerStyle.Alert);
                    /// 点击确定
                    alertController.AddAction(UIAlertAction.Create("Ok", UIAlertActionStyle.Default, (action) => ctx.Complete(Unit.Default)));
                    vc.PresentViewController(alertController, true, null);
                }
                else
                {
                    /// 默认行为, 不做理会
                    ctx.Complete(unit);
                }
            });
            completionHandler();
        }
        [Export("webView:runJavaScriptConfirmPanelWithMessage:initiatedByFrame:completionHandler:")]
        public override async void RunJavaScriptConfirmPanel(WKWebView webView, string message, WKFrameInfo frame, Action<bool> completionHandler)
        {
            var (confirm, _) = await dWebView.JsConfirmSignal.EmitForResult((webView, message, frame), async (args, ctx, _) =>
            {

                if (webView.GetUIViewController() is not null and var vc)
                {
                    var confirmControlelr = UIAlertController.Create(args.webView.Title, args.message, UIAlertControllerStyle.Alert);
                    confirmControlelr.AddAction(UIAlertAction.Create("Cancel", UIAlertActionStyle.Cancel, (action) => ctx.Complete(false)));
                    confirmControlelr.AddAction(UIAlertAction.Create("Ok", UIAlertActionStyle.Default, (action) => ctx.Complete(true)));
                    vc.PresentViewController(confirmControlelr, true, null);
                }
                else
                {
                    /// 默认行为, 返回false
                    ctx.Complete(false);
                }

            });

            completionHandler(confirm);
        }
        [Export("webView:runJavaScriptTextInputPanelWithPrompt:defaultText:initiatedByFrame:completionHandler:")]
        public override async void RunJavaScriptTextInputPanel(WKWebView webView, string prompt, string? defaultText, WKFrameInfo frame, Action<string> completionHandler)
        {
            var (promptText, _) = await dWebView.JsPromptSignal.EmitForResult((webView, prompt, defaultText, frame), async (args, ctx, _) =>
            {
                if (webView.GetUIViewController() is not null and var vc)
                {

                    var confirmControlelr = UIAlertController.Create(args.webView.Title, args.prompt, UIAlertControllerStyle.Alert);
                    confirmControlelr.AddTextField((textField) =>
                    {
                        textField.Text = args.defaultText;
                        textField.SelectAll(null);
                        confirmControlelr.AddAction(UIAlertAction.Create("Cancel", UIAlertActionStyle.Cancel, (action) => ctx.Complete(null)));
                        confirmControlelr.AddAction(UIAlertAction.Create("Ok", UIAlertActionStyle.Default, (action) => ctx.Complete(textField.Text)));
                        vc.PresentViewController(confirmControlelr, true, null);
                    });
                }
                else
                {
                    /// 默认行为, 返回 js-null
                    ctx.Complete(null);
                }
            });

            /// TODO 未来升级后，这里就可空了
            completionHandler(promptText ?? "");
        }

        [Export("webView:requestMediaCapturePermissionForOrigin:initiatedByFrame:type:decisionHandler:")]
        public override async void RequestMediaCapturePermission(WKWebView webView, WKSecurityOrigin origin, WKFrameInfo frame, WKMediaCaptureType type, Action<WKPermissionDecision> decisionHandler)
        {
            var mediaTypes = type switch
            {
                WKMediaCaptureType.Camera => new[] { AVAuthorizationMediaType.Video },
                WKMediaCaptureType.Microphone => new[] { AVAuthorizationMediaType.Audio },
                WKMediaCaptureType.CameraAndMicrophone => new[] { AVAuthorizationMediaType.Video, AVAuthorizationMediaType.Audio },
                _ => Array.Empty<AVAuthorizationMediaType>()
            };

            if (mediaTypes.Length is 0)
            {
                decisionHandler(WKPermissionDecision.Prompt);
                return;
            }

            foreach (var mediaType in mediaTypes)
            {
                bool? isAuthorized = AVCaptureDevice.GetAuthorizationStatus(mediaType) switch
                {
                    AVAuthorizationStatus.NotDetermined => await AVCaptureDevice.RequestAccessForMediaTypeAsync(mediaType),
                    AVAuthorizationStatus.Authorized => true,
                    AVAuthorizationStatus.Denied => false,
                    /// 1. 家长控制功能启用,限制了应用访问摄像头或麦克风
                    /// 2. 机构部署的设备,限制了应用访问硬件功能
                    /// 3. 用户在 iCloud 中的"隐私"设置中针对应用禁用了访问权限
                    AVAuthorizationStatus.Restricted => null,
                    _ => null,
                };
                /// 认证失败
                if (isAuthorized is false)
                {
                    decisionHandler(WKPermissionDecision.Deny);
                    return;
                }
                /// 受限 或者 未知？
                /// TODO 用额外的提示框提示用户
                if (isAuthorized is null)
                {
                    decisionHandler(WKPermissionDecision.Prompt);
                    return;
                }
            }
            /// 所有的权限都验证通过
            decisionHandler(WKPermissionDecision.Grant);

        }
    }
}

