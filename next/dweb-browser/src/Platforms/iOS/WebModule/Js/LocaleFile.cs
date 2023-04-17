using DwebBrowser.MicroService.Core;
using DwebBrowser.Helper;
using System.Net;
using System.Web;
using System.IO;

namespace DwebBrowser.WebModule.Js;

public static class LocaleFile
{
    public static string AssetRootPath()
    {
        var currentPath = Directory.GetCurrentDirectory();
        return "";
    }

    public static HttpResponseMessage? LocaleFileFetch(MicroModule remote, HttpRequestMessage request)
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

            /// 尝试打开文件，如果打开失败就走 404 no found 响应
            //var filenameList = 

            return new HttpResponseMessage(HttpStatusCode.OK);
        }

        return null;
    }
}

