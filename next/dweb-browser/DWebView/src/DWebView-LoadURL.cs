using System.Runtime.Versioning;
using DwebBrowser.Helper;
using DwebBrowser.MicroService.Core;
using DwebBrowser.MicroService.Http;
using DwebBrowser.MicroService.Message;
using DwebBrowser.MicroService.Sys.Http;
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
            return GetScheme(url.GetFullAuthority());
        }
        public static string GetScheme(string host)
        {
            return host.Replace(":", "+");
        }
        public DwebSchemeHandler(MicroModule microModule, Uri url)
        {
            this.microModule = microModule;
            this.baseUri = url;
            this.host = url.GetFullAuthority();
            this.scheme = GetScheme(host);
        }


        public class NSStream : Stream
        {
            NSInputStream _nsStream;

            public NSStream(NSInputStream inputStream)
            {
                this._nsStream = inputStream;
            }

            public override bool CanRead => true;

            public override bool CanSeek => false;

            public override bool CanWrite => false;

            public override long Length => throw new NotSupportedException();

            public override long Position { get => throw new NotSupportedException(); set => throw new NotSupportedException(); }

            public override void Flush()
            {
                throw new NotSupportedException();
            }

            public override int Read(byte[] buffer, int offset, int count)
            {
                if (offset != 0)
                    throw new NotSupportedException();
                return (int)_nsStream.Read(buffer, offset, (uint)count);
            }

            public override long Seek(long offset, SeekOrigin origin)
            {
                throw new NotSupportedException();
            }

            public override void SetLength(long value)
            {
                throw new NotSupportedException();
            }

            public override void Write(byte[] buffer, int offset, int count)
            {
                throw new NotSupportedException();
            }
        }
        [Export("webView:startURLSchemeTask:")]
        [SupportedOSPlatform("ios11.0")]
        public async void StartUrlSchemeTask(WKWebView webView, IWKUrlSchemeTask urlSchemeTask)
        {
            try
            {
                Console.Log("StartUrlSchemeTask", "Start: {0}", urlSchemeTask.Request.Url.AbsoluteString);
                var url = new Uri(this.baseUri, urlSchemeTask.Request.Url.ResourceSpecifier);
                /// 构建请求
                var pureRequest = new PureRequest(
                    url.AbsoluteUri,
                    IpcMethod.From(urlSchemeTask.Request.HttpMethod),
                    /// 构建请求的 Headers
                    urlSchemeTask.Request.Headers.Select((kv) =>
                    {
                        return KeyValuePair.Create((string)(NSString)kv.Key, (string)(NSString)kv.Value);
                    }).ToIpcHeaders(),
                    /// 构建请求的 ContentBody
                    urlSchemeTask.Request.BodyStream switch
                    {
                        null => null,
                        var nsBodyStream => new PureStreamBody(new NSStream(nsBodyStream))
                    });

                /// 获得响应
                var pureResponse = await microModule.NativeFetchAsync(pureRequest);
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

