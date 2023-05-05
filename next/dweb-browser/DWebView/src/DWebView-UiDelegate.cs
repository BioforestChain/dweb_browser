using System;
using AVFoundation;
using DwebBrowser.Helper;
using UIKit;
using WebKit;

namespace DwebBrowser.DWebView;

public partial class DWebView : WKWebView
{
    public event Signal<(WKWebView webView, WKWebViewConfiguration configuration, WKNavigationAction navigationAction, WKWindowFeatures windowFeatures, Action<WKWebView?> completionHandler)>? OnCreateWebView;
    public event Signal<WKWebView>? OnClose;
    public event Signal<(WKWebView webView, string message, WKFrameInfo frame, Action completionHandler)>? OnAlert;
    public event Signal<(WKWebView webView, string message, WKFrameInfo frame, Action<bool> completionHandler)>? OnConfirm;
    public event Signal<(WKWebView webView, string prompt, string? defaultText, WKFrameInfo frame, Action<string> completionHandler)>? OnPrompt;


    class DWebViewUiDelegate : WKUIDelegate
    {
        DWebView dWebView;
        internal DWebViewUiDelegate(DWebView dWebView)
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
            (dWebView.OnCreateWebView?.Emit(args))?.Wait();
            return result;
        }
        [Export("webViewDidClose:")]
        public override void DidClose(WKWebView webView)
        {
            dWebView.OnClose?.Emit(webView)?.Wait();
        }


        [Export("webView:runJavaScriptAlertPanelWithMessage:initiatedByFrame:completionHandler:")]
        public override void RunJavaScriptAlertPanel(WKWebView webView, string message, WKFrameInfo frame, Action _completionHandler)
        {
            var complete = false;
            var completionHandler = () =>
            {
                complete = true;
                _completionHandler();
            };
            var args = (webView, message, frame, completionHandler);
            (dWebView.OnAlert?.Emit(args))?.Wait();
            if (!complete)
            {
                /// 默认行为, 不做理会
                completionHandler();
            }
        }
        [Export("webView:runJavaScriptConfirmPanelWithMessage:initiatedByFrame:completionHandler:")]
        public override void RunJavaScriptConfirmPanel(WKWebView webView, string message, WKFrameInfo frame, Action<bool> _completionHandler)
        {

            var complete = false;
            var completionHandler = (bool confirm) =>
            {
                complete = true;
                _completionHandler(confirm);
            };
            var args = (webView, message, frame, completionHandler);
            (dWebView.OnConfirm?.Emit(args))?.Wait();
            if (!complete)
            {
                /// 默认行为, 不做理会
                completionHandler(false);
            }
        }
        [Export("webView:runJavaScriptTextInputPanelWithPrompt:defaultText:initiatedByFrame:completionHandler:")]
        public override void RunJavaScriptTextInputPanel(WKWebView webView, string prompt, string? defaultText, WKFrameInfo frame, Action<string> _completionHandler)
        {
            var complete = false;
            var completionHandler = (string text) =>
            {
                complete = true;
                _completionHandler(text);
            };
            var args = (webView, prompt, defaultText, frame, completionHandler);
            (dWebView.OnPrompt?.Emit(args))?.Wait();
            if (!complete)
            {
                /// 默认行为, 不做理会
                completionHandler(defaultText ?? "");
            }
        }

        [Export("webView:requestMediaCapturePermissionForOrigin:initiatedByFrame:type:decisionHandler:")]
        public override async void RequestMediaCapturePermission(WKWebView webView, WKSecurityOrigin origin, WKFrameInfo frame, WKMediaCaptureType type, Action<WKPermissionDecision> decisionHandler)
        {
            var mediaTypes = type switch
            {
                WKMediaCaptureType.Camera => new[] { AVAuthorizationMediaType.Video },
                WKMediaCaptureType.Microphone => new[] { AVAuthorizationMediaType.Audio },
                WKMediaCaptureType.CameraAndMicrophone => new[] { AVAuthorizationMediaType.Video, AVAuthorizationMediaType.Audio },
                _ => new AVAuthorizationMediaType[0]
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

