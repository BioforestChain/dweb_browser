using System;
using System.Web;
using System.Net;

namespace DwebBrowser.MicroService.Core;

using FetchAdapter = Func<MicroModule, HttpRequestMessage, Task<HttpResponseMessage?>>;

public class NativeFetch
{
	public NativeFetch()
	{
	}

	public static AdapterManager<FetchAdapter> NativeFetchAdaptersManager = new();
}

public abstract partial class MicroModule
{
    public async Task<HttpResponseMessage> NativeFetchAsync(HttpRequestMessage request)
    {
        foreach (var fetchAdapter in NativeFetch.NativeFetchAdaptersManager.Adapters)
        {
            var response = await fetchAdapter(this, request);

            if (response is not null)
            {
                return response;
            }
        }

        return _localeFileFetch(this, request) ?? await new HttpClient().SendAsync(request);
    }

    private HttpResponseMessage? _localeFileFetch(MicroModule remote, HttpRequestMessage request)
    {
        if (request.RequestUri is not null && request.RequestUri.Scheme == "file" && request.RequestUri.Host == "")
        {
            var query = HttpUtility.ParseQueryString(request.RequestUri.Query);

            var mode = query["mode"] ?? "auto";
            var chunk = query["chunk"]?.ToIntOrNull() ?? 1024 * 1024;
            var preRead = query["pre-read"]?.ToBooleanStrictOrNull() ?? false;

            var src = request.RequestUri.AbsolutePath.Substring(1);

            Console.WriteLine($"OPEN {src}");
            string dirname = null!;
            string filename = null!;

            src.LastIndexOf('/').Also(it =>
            {
                switch (it)
                {
                    case -1:
                        filename = src;
                        dirname = "";
                        break;
                    default:
                        filename = src.Substring(it + 1);
                        dirname = src.Substring(0, it + 1);
                        break;
                }
                src.Substring(0, it + 1);
            });

            // TODO: nativeFetch 本地文件读取未完成
            /// 尝试打开文件，如果打开失败就走 404 no found 响应

            return new HttpResponseMessage(HttpStatusCode.OK);
        }

        return null;
    }

    public Task<HttpResponseMessage> NativeFetchAsync(Uri url) =>
        NativeFetchAsync(new HttpRequestMessage(HttpMethod.Get, url));

    public Task<HttpResponseMessage> NativeFetchAsync(string url) =>
        NativeFetchAsync(new HttpRequestMessage(HttpMethod.Get, new Uri(url)));
}