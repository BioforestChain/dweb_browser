using System;
using System.Net.Http.Headers;
using System.Reflection.PortableExecutable;
using System.Runtime.Versioning;
using AngleSharp;
using AngleSharp.Html.Parser;
using AVFoundation;
using DwebBrowser.Helper;
using DwebBrowser.MicroService.Core;
using DwebBrowser.MicroService.Message;
using DwebBrowser.MicroService.Sys.Http;
using SystemConfiguration;
using WebKit;

namespace DwebBrowser.DWebView;

public partial class DWebView : WKWebView
{

    public class DwebSchemeHandler : NSObject, IWKUrlSchemeHandler
    {
        MicroModule microModule;
        public Uri baseUri;
        public string host;
        public string scheme;
        public static string GetScheme(Uri url)
        {
            return GetScheme(GetHost(url));
        }
        public static string GetScheme(string host)
        {
            return host.Replace(":", "+");
        }
        public static string GetHost(Uri url)
        {
            var host = url.Authority;
            if (host.Contains(":") is false)
            {
                host += url.Scheme is "https" ? ":443" : ":80";
            }
            return host;
        }
        public DwebSchemeHandler(MicroModule microModule, Uri url)
        {
            this.microModule = microModule;
            this.baseUri = url;
            this.host = GetHost(url);
            this.scheme = GetScheme(host);
        }


        public class InputStreamStream : Stream
        {
            NSInputStream inputStream;

            public InputStreamStream(NSInputStream inputStream)
            {
                this.inputStream = inputStream;
            }

            public override bool CanRead => throw new NotImplementedException();

            public override bool CanSeek => throw new NotImplementedException();

            public override bool CanWrite => throw new NotImplementedException();

            public override long Length => throw new NotImplementedException();

            public override long Position { get => throw new NotImplementedException(); set => throw new NotImplementedException(); }

            public override void Flush()
            {
                throw new NotImplementedException();
            }

            public override int Read(byte[] buffer, int offset, int count)
            {
                return (int)inputStream.Read(buffer, offset, (uint)count);
            }

            public override long Seek(long offset, SeekOrigin origin)
            {
                throw new NotImplementedException();
            }

            public override void SetLength(long value)
            {
                throw new NotImplementedException();
            }

            public override void Write(byte[] buffer, int offset, int count)
            {
                throw new NotImplementedException();
            }
        }
        [Export("webView:startURLSchemeTask:")]
        [SupportedOSPlatform("ios11.0")]
        public async void StartUrlSchemeTask(WKWebView webView, IWKUrlSchemeTask urlSchemeTask)
        {
            try
            {
                Console.Log("StartUrlSchemeTask", "Start: {0}", urlSchemeTask.Request.Url.AbsoluteString);
                var url = new Uri(this.baseUri, urlSchemeTask.Request.Url.Path);
                using var request = new HttpRequestMessage(new(urlSchemeTask.Request.HttpMethod), url.AbsoluteUri);
                if (urlSchemeTask.Request.BodyStream is not null and var nsBodyStream)
                {
                    request.Content = new StreamContent(new InputStreamStream(nsBodyStream));
                }

                var response = await microModule.NativeFetchAsync(request);
                var nsStatusCode = new IntPtr((int)response.StatusCode);
                var nsHeaders = new NSMutableDictionary<NSString, NSString>();
                foreach (var (key, values) in response.Headers)
                {
                    var value = values.FirstOrDefault();
                    if (value is not null)
                    {
                        nsHeaders.Add(new NSString(key), new NSString(value));
                    }
                }
                var mimeType = "application/octet-stream";
                string? textEncodingName = null;
                var contentLength = (response.Content.Headers.ContentLength ?? -1).ToInt();
                var contentType = response.Content.Headers.ContentType;
                if (contentType is not null)
                {
                    mimeType = contentType.MediaType;
                    textEncodingName = contentType.CharSet;
                }
                using var nsResponse = new NSUrlResponse(urlSchemeTask.Request.Url, mimeType, new IntPtr(contentLength), textEncodingName);
                //using var nsResponse = new NSHttpUrlResponse(urlSchemeTask.Request.Url, nsStatusCode, "HTTP/1.1", nsHeaders);
                urlSchemeTask.DidReceiveResponse(nsResponse);
                //new NSUrlResponse

                // 将响应体发送到urlSchemeTask
                using (var stream = await response.StreamAsync())
                {
                    await foreach (var bytes in stream.ReadBytesStream())
                    {
                        urlSchemeTask.DidReceiveData(NSData.FromArray(bytes));
                    }
                }

                urlSchemeTask.DidFinish();
                Console.Log("StartUrlSchemeTask", "End: {0}", urlSchemeTask.Request.Url.AbsoluteString);
            }
            catch (Exception err)
            {
                using var nsResponse = new NSHttpUrlResponse(urlSchemeTask.Request.Url, new IntPtr(502), "HTTP/1.1", new());
                urlSchemeTask.DidReceiveResponse(nsResponse);
                urlSchemeTask.DidReceiveData(NSData.FromString(err.Message));
                urlSchemeTask.DidFinish();
            }
        }

        [Export("webView:stopURLSchemeTask:")]
        public void StopUrlSchemeTask(WKWebView webView, IWKUrlSchemeTask urlSchemeTask)
        {
            urlSchemeTask.DidFinish();
        }
    }



    public Task LoadURL(string url) => LoadURL(new Uri(url));

    //HtmlParser htmlParser = new HtmlParser(new()
    //{
    //});

    /// <summary>
    /// 尝试将 .dweb 这样的域名注册成 协议
    /// </summary>
    /// <param name="url"></param>
    /// <param name="remoteMM"></param>
    /// <param name="configuration"></param>
    static void TryRegistryUrlSchemeHandler(Uri url, MicroModule remoteMM, WKWebViewConfiguration configuration)
    {

        if (url.Host.EndsWith(".dweb") && url.Scheme is "http" or "https")
        {
            var dwebSchemeHandler = new DwebSchemeHandler(remoteMM, url);
            configuration.SetUrlSchemeHandler(dwebSchemeHandler, dwebSchemeHandler.scheme);
        }
    }

    public async Task LoadURL(Uri url, HttpMethod? method = default)
    {
        WKNavigation? wkNavigation;

        /// 如果是 dweb 域名，这是需要加入网关的链接前缀才能被正常加载
        if (url.Host.EndsWith(".dweb") && url.Scheme is "http" or "https")
        {
            /// 这个域名是通过buildInternal得来的
            var internalUrl = url;

            if (options.AllowDwebScheme &&
                /// 获取协议头，确保这个协议头是注册过的
                DwebSchemeHandler.GetScheme(internalUrl) is var dwebScheme && this.Configuration.GetUrlSchemeHandler(dwebScheme) is not null)
            {
                /// 如果注册了协议头，那么直接走自定义拦截
                url = new Uri(dwebScheme + ":" + internalUrl.PathAndQuery);
            }
            else
            {
                /// 作为base的href，我们需要确保是标准的http请求，所以这个域名在前缀加上“网关”，确保是 http 域名
                var baseUrl = new Uri(internalUrl.ToPublicDwebHref());
                /// 走 http 网关
                url = baseUrl;
            }



            ///// 我们需要确保最终显示的链接必须是非安全的，因为IOS的安全策略
            //var unsafeUrl = new Uri(url.AbsoluteUri).SetSchema("http");

            //#region 这里我们通过 ipc 获得 http 的响应，然后为其强制填充 base-href
            //using var request = new HttpRequestMessage(method ?? HttpMethod.Get, internalUrl);
            //using var response = await remoteMM.NativeFetchAsync(request);

            //using var nsUrlRequest = new NSUrlRequest(new NSUrl(internalUrl.AbsoluteUri));
            //using var nsUrlResponse = new NSUrlResponse(nsUrlRequest.Url, response.Content.Headers.ContentType?.MediaType ?? "application/octet-stream", new IntPtr(response.Content.Headers.ContentLength ?? 0), response.Content.Headers.ContentType?.CharSet);
            //string responseData = await response.Content.ReadAsStringAsync() ?? "";

            //try
            //{
            //    var document = await htmlParser.ParseDocumentAsync(responseData);
            //    var baseNode = document.Head?.QuerySelector("base");
            //    if (baseNode is null)
            //    {
            //        baseNode = document.CreateElement("base");
            //        document.Head!.InsertBefore(baseNode, document.Head.FirstChild);
            //    }

            //    /// 然后，我们强制注入(或篡改) <base href/>
            //    string origin = baseNode.GetAttribute("href")?.Let((href) => new Uri(url, href).ToString()) ?? internalUrl.AbsoluteUri;
            //    string gatewayOrigin = HttpNMM.DwebServer.Origin;
            //    if (!origin.StartsWith(gatewayOrigin))
            //    {
            //        baseNode.SetAttribute("href", baseUrl.AbsoluteUri);
            //        responseData = document.ToHtml();
            //    }
            //}
            //finally { }
            //#endregion

            ///// 最终，我们进行模拟加载
            //var nsData = NSData.FromString(responseData);
            //wkNavigation = LoadSimulatedRequest(new NSUrlRequest(new NSUrl(baseUrl.AbsoluteUri)), nsUrlResponse, nsData);
        }

        string uri = url.ToString() ?? throw new ArgumentException();
        var nsUrlRequest = new NSUrlRequest(new NSUrl(uri));
        wkNavigation = LoadRequest(nsUrlRequest);


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

