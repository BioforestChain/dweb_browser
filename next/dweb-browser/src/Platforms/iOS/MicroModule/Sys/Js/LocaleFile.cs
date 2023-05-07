using DwebBrowser.MicroService.Core;
using DwebBrowser.Helper;
using System.Net;
using System.Web;
using Foundation;
using MobileCoreServices;
using System.Net.Http.Headers;
using CoreMedia;
using Microsoft.AspNetCore.StaticFiles;
using Microsoft.Maui.Storage;
using static CoreFoundation.DispatchSource;

#nullable enable

namespace DwebBrowser.MicroService.Sys.Js;

public static class LocaleFile
{
    static Debugger Console = new Debugger("LocaleFile");
    /// <summary>
    /// 应用根目录
    /// </summary>
    /// <returns></returns>
    public static string RootPath() => AppContext.BaseDirectory;

    /// <summary>
    /// 资源文件目录
    /// </summary>
    /// <returns></returns>
    public static string AssetsPath() => Path.Combine(RootPath(), "Assets");

    /// <summary>
    /// 获取文件MimeType
    /// </summary>
    /// <param name="filepath"></param>
    /// <returns></returns>
    public static string GetMimeType(string fileName)
    {
        var provider = new FileExtensionContentTypeProvider();
        if (!provider.TryGetContentType(fileName, out var contentType))
        {
            contentType = "application/octet-stream";
        }
        return contentType;
    }

    public static async Task<HttpResponseMessage?> LocaleFileFetch(MicroModule remote, HttpRequestMessage request)
    {
        try
        {
            if (request.RequestUri is not null && request.RequestUri.Scheme == "file" && request.RequestUri.Host == "")
            {
                var query = HttpUtility.ParseQueryString(request.RequestUri.Query);

                var mode = query["mode"] ?? "auto";
                var chunk = query["chunk"]?.ToIntOrNull() ?? 1024 * 1024;
                //var preRead = query["pre-read"]?.ToBooleanStrictOrNull() ?? false;

                var src = request.RequestUri.AbsolutePath.Substring(1); // 移除 '/'

                Console.Log("LocaleFileFetch", "OPEN {0}", src);
                string dirname = Path.GetDirectoryName(src) ?? "";
                string filename = Path.GetFileName(src) ?? "";


                /// 尝试打开文件，如果打开失败就走 404 no found 响应
                var absoluteDir = Path.Combine(AssetsPath(), dirname);
                var absoluteDirFiles = new string[0].Try((arr) => arr.Concat(Directory.GetFileSystemEntries(absoluteDir)).ToArray());


                var absoluteFile = Path.Combine(absoluteDir, filename);

                /// 文件不存在
                if (absoluteDirFiles.Contains(absoluteFile) is false)
                {
                    Console.Log("LocaleFileFetch", "NO-FOUND {0}", request.RequestUri.AbsolutePath);
                    var notFoundResponse = new HttpResponseMessage(HttpStatusCode.NotFound).Also(it =>
                    {
                        it.Content = new StringContent(String.Format("the file({0}) not found.", request.RequestUri.AbsolutePath));
                    });
                    return notFoundResponse;
                }


                /// 开始读取文件来响应内容

                var okResponse = new HttpResponseMessage(HttpStatusCode.OK);
                var fs = File.OpenRead(absoluteFile);
                Console.Log("LocaleFileFetch", "Mode: {0}", mode);

                // buffer 模式，就是直接全部读取出来
                // TODO auto 模式就是在通讯次数和单次通讯延迟之间的一个取舍，如果分片次数少于2次，那么就直接发送，没必要分片
                if (mode is "stream")
                {
                    Task.Run(async () =>
                    {
                        var fs = File.OpenRead(absoluteFile);
                        var z = new StreamContent(fs, (int)fs.Length);
                        var s = await z.ReadAsStreamAsync();
                        Task.Run(async () =>
                        {
                            await Task.Delay(1000);
                            Console.Log("LocaleFileFetch", "Start Read Test: {0}", s);
                            await foreach (var data in s.ReadBytesStream(chunk))
                            {
                                Console.Log("LocaleFileFetch", "Read Test: {0}", data.Length);
                            }
                            Console.Log("LocaleFileFetch", "End Read Test: {0}", s);
                        }).Background();

                    }).Background();
                    /// 返回流
                    okResponse.Content = new StreamContent(fs, (int)fs.Length);
                }
                else using (fs)
                    {
                        /// 一次性发送
                        okResponse.Content = new ByteArrayContent(await fs.ReadBytesAsync(fs.Length));
                    }

                okResponse.Content.Headers.ContentType = new MediaTypeHeaderValue(GetMimeType(filename));
                return okResponse;
            }
        }
        catch (Exception e)
        {
            Console.Warn("LocaleFileFetch", "Exception: {0}", e.Message);
            return new HttpResponseMessage(HttpStatusCode.InternalServerError);
        }

        return null;
    }
}

