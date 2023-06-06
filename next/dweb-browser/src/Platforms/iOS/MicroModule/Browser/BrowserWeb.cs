using WebKit;
using Foundation;
using CoreGraphics;
using System.Runtime.Versioning;
using DwebBrowser.MicroService.Http;
using DwebBrowser.MicroService.Core;
using DwebBrowser.MicroService.Sys.Http;

namespace DwebBrowser.MicroService.Browser;

public partial class BrowserWeb : WKWebView
{
    public BrowserWeb(CGRect frame, WKWebViewConfiguration configuration) : base(frame, configuration)
    {
        configuration.SetUrlSchemeHandler(new DwebSchemeHandler(), "dweb");
    }
}

#region 添加拦截dweb-deeplinks
public class DwebSchemeHandler : NSObject, IWKUrlSchemeHandler
{
    [Export("webView:startURLSchemeTask:")]
    [SupportedOSPlatform("ios11.0")]
    async void IWKUrlSchemeHandler.StartUrlSchemeTask(WKWebView webView, IWKUrlSchemeTask urlSchemeTask)
    {

        if (urlSchemeTask.Request.Url.Scheme == "dweb")
        {
            await MainThread.InvokeOnMainThreadAsync(async () =>
            {
                /// 获得响应
                var pureResponse = await BrowserNMM.BrowserController?.BrowserNMM.NativeFetchAsync(
                    new PureRequest(
                        urlSchemeTask.Request.Url.AbsoluteString,
                        IpcMethod.Get,
                        /// 构建请求的 Headers
                        urlSchemeTask.Request.Headers.Select((kv) =>
                        {
                            return KeyValuePair.Create((string)(NSString)kv.Key, (string)(NSString)kv.Value);
                        }).ToIpcHeaders()));

                /// 获得响应的状态码
                var nsStatusCode = new IntPtr((int)pureResponse.StatusCode);
                /// 构建响应的头部
                var nsHeaders = new NSMutableDictionary<NSString, NSString>();
                foreach (var (key, value) in pureResponse.Headers)
                {
                    nsHeaders.Add(new NSString(key), new NSString(value));
                }
                using var nsResponse = new NSHttpUrlResponse(urlSchemeTask.Request.Url, nsStatusCode, "HTTP/1.1", nsHeaders);

                /// 写入响应头
                urlSchemeTask.DidReceiveResponse(nsResponse);

                // 写入响应体：将响应体发送到urlSchemeTask
                switch (pureResponse.Body)
                {
                    case PureEmptyBody: break;
                    case PureStreamBody streamBody:
                        await foreach (var chunk in streamBody.Data.ReadBytesStream())
                        {
                            urlSchemeTask.DidReceiveData(NSData.FromArray(chunk));
                        }
                        break;
                    case PureBody body:
                        var data = body.ToByteArray();
                        if (data.Length > 0)
                        {
                            urlSchemeTask.DidReceiveData(NSData.FromArray(data));
                        }
                        break;
                };

                /// 写入完成
                urlSchemeTask.DidFinish();
            });
        }
    }

    [Export("webView:stopURLSchemeTask:")]
    void IWKUrlSchemeHandler.StopUrlSchemeTask(WKWebView webView, IWKUrlSchemeTask urlSchemeTask)
    {
        urlSchemeTask.DidFinish();
    }
}
#endregion
