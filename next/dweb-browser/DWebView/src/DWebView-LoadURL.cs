using System;
using System.Runtime.Versioning;
using AngleSharp.Html.Parser;
using DwebBrowser.Helper;
using DwebBrowser.MicroService.Core;
using DwebBrowser.MicroService.Sys.Http;
using WebKit;

namespace DwebBrowser.DWebView;

public partial class DWebView : WKWebView
{

    //public class DwebSchemeHandler : NSObject, IWKUrlSchemeHandler
    //{
    //    MicroModule microModule;
    //    public DwebSchemeHandler(MicroModule microModule)
    //    {
    //        this.microModule = microModule;
    //    }
    //    [Export("webView:startURLSchemeTask:")]
    //    [SupportedOSPlatform("ios11.0")]
    //    public async void StartUrlSchemeTask(WKWebView webView, IWKUrlSchemeTask urlSchemeTask)
    //    {
    //        if (urlSchemeTask.Request.HttpMethod is "GET" && urlSchemeTask.Request.Url.Path?.StartsWith(HttpNMM.X_DWEB_HREF) is true)
    //        {
    //            var request = new HttpRequestMessage(HttpMethod.Get, urlSchemeTask.Request.Url.Path.Substring(HttpNMM.X_DWEB_HREF.Length));
    //            //var request = new HttpRequestMessage(HttpMethod.Get, "file://http.sys.dweb" + urlSchemeTask.Request.Url.Path);
    //            var response = await microModule.NativeFetchAsync(request);
    //            //using var response = new NSHttpUrlResponse(urlSchemeTask.Request.Url, statusCode, "HTTP/1.1", dic);
    //            var nsStatusCode = new IntPtr((int)response.StatusCode);
    //            var nsHeaders = new NSMutableDictionary<NSString, NSString>();
    //            foreach (var (key, values) in response.Headers)
    //            {
    //                var value = values.FirstOrDefault();
    //                if (value is not null)
    //                {
    //                    nsHeaders.Add(new NSString(key), new NSString(value));
    //                }
    //            }

    //            using var nsResponse = new NSHttpUrlResponse(urlSchemeTask.Request.Url, nsStatusCode, "HTTP/1.1", nsHeaders);
    //            urlSchemeTask.DidReceiveResponse(nsResponse);


    //            // 将响应体发送到urlSchemeTask
    //            using (var stream = await response.StreamAsync())
    //            {
    //                await foreach (var bytes in stream.ReadBytesStream())
    //                {
    //                    urlSchemeTask.DidReceiveData(NSData.FromArray(bytes));
    //                }
    //            }
    //            urlSchemeTask.DidFinish();
    //        }
    //        else
    //        {
    //            urlSchemeTask.DidFinish();
    //        }
    //    }

    //    [Export("webView:stopURLSchemeTask:")]
    //    public void StopUrlSchemeTask(WKWebView webView, IWKUrlSchemeTask urlSchemeTask)
    //    {
    //    }
    //}



    public Task LoadURL(string url) => LoadURL(new Uri(url));

    //HtmlParser htmlParser = new HtmlParser();

    public async Task LoadURL(Uri url, HttpMethod? method = default)
    {

        /// 如果是 dweb 域名，这是需要加入网关的链接前缀才能被正常加载
        if (url.Host.EndsWith(".dweb") && url.Scheme is "http" or "https")
        {
            url = new Uri(url.ToPublicDwebHref());
            //using var request = new HttpRequestMessage(method ?? HttpMethod.Get, url);
            //using var response = await remoteMM.NativeFetchAsync(request);

            //using var nsUrlResponse = new NSUrlResponse(nsUrlRequest.Url, response.Content.Headers.ContentType?.MediaType ?? "application/octet-stream", new IntPtr(response.Content.Headers.ContentLength ?? 0), response.Content.Headers.ContentType?.CharSet);
            //string responseData = await response.Content.ReadAsStringAsync() ?? "";

            //var document = htmlParser.ParseDocument(responseData);
            //var baseNode = document.Head?.QuerySelector("base");
            //if (baseNode is null)
            //{
            //    baseNode = document.CreateElement("base");
            //    document.Head!.InsertBefore(baseNode, document.Head.FirstChild);
            //}
            //string origin = baseNode.GetAttribute("href")?.Let((href) => new Uri(url, href).ToString()) ?? uri;
            //string gatewayOrigin = HttpNMM.DwebServer.Origin; // "dweb:";
            //if (!origin.StartsWith(gatewayOrigin))
            //{
            //    baseNode.SetAttribute("href", gatewayOrigin + HttpNMM.X_DWEB_HREF + origin);
            //    responseData = document.ToHtml();
            //}

            ///// 模拟加载
            //var nsData = NSData.FromString(responseData);
            //nsNavigation = LoadSimulatedRequest(nsUrlRequest, nsUrlResponse, nsData);
        }

        string uri = url.ToString() ?? throw new ArgumentException();
        var nsUrlRequest = new NSUrlRequest(new NSUrl(uri));
        WKNavigation? nsNavigation;
        nsNavigation = LoadRequest(nsUrlRequest);


        if (OnReady is not null && !OnReady.IsEmpty())
        {
            this.NavigationDelegate = new OnReadyDelegate(OnReady);
        }
    }

    public event Signal? OnReady;

    class OnReadyDelegate : WKNavigationDelegate
    {
        Signal _onReady;
        public OnReadyDelegate(Signal onReady)
        {
            this._onReady = onReady;
        }
        public override void DidStartProvisionalNavigation(WKWebView webView, WKNavigation navigation)
        {
            Console.Log("WKNavigationDelegate", "Started provisional navigation");
        }

        public override void DidFinishNavigation(WKWebView webView, WKNavigation navigation)
        {
            //base.DidFinishNavigation(webView, navigation);
            _ = _onReady.Emit();
        }
    }

}

