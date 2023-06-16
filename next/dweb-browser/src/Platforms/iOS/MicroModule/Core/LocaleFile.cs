using System.Net;
using System.Web;
using DwebBrowser.MicroService.Browser.Jmm;
using DwebBrowser.MicroService.Http;
using Microsoft.AspNetCore.StaticFiles;

#nullable enable

namespace DwebBrowser.MicroService.Core;

public static class LocaleFile
{
    static readonly Debugger Console = new("LocaleFile");
    /// <summary>
    /// 需要一些初始化调用，才能让LocaleFile模块正的运作起来
    /// </summary>
    public static void Init()
    {
        Console.Log("Init", "init!!!");
    }

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
    static LocaleFile()
    {
        // 将本地资源文件读取添加到适配器中
        NativeFetch.NativeFetchAdaptersManager.Append(LocaleFileFetch);
    }

    public static async Task<PureResponse?> LocaleFileFetch(MicroModule remote, PureRequest request)
        => request.ParsedUrl?.Scheme switch
        {
            string schema when schema == Uri.UriSchemeFile => await GetLocalFetch(remote, request),
            _ => null
        };

    public static async Task<PureResponse?> GetLocalFetch(MicroModule remote, PureRequest request)
    {
        try
        {
            if (request.ParsedUrl is not null and var parsedUrl && parsedUrl.Scheme is "file" && parsedUrl.FullHost is "")
            {
                var query = HttpUtility.ParseQueryString(parsedUrl.Query);
                var mode = query["mode"] ?? "auto";
                var chunk = query["chunk"]?.ToIntOrNull() ?? 1024 * 1024;

                var src = string.Empty;
                var dirname = string.Empty;

                if (parsedUrl.Path.StartsWith("/usr/"))
                {
                    src = parsedUrl.Path;
                    dirname = Path.Combine(JmmDwebService.DWEB_APP_DIR, remote.Mmid);
                } else if (parsedUrl.Path.StartsWith("/sys/"))
                {
                    src = parsedUrl.Path[5..]; // 移除 '/sys/'
                    dirname = PathHelper.GetIOSAppAssetsPath();
                }

                Console.Log("LocaleFileFetch", "OPEN {0}", src);

                /// 尝试打开文件，如果打开失败就走 404 no found 响应
                var absoluteDir = Path.Join(dirname, Path.GetDirectoryName(src) ?? "");
                var absoluteDirFiles = new string[0].Try((arr) => arr.Concat(Directory.GetFileSystemEntries(absoluteDir)).ToArray());

                var filename = Path.GetFileName(src) ?? "";
                var absoluteFile = Path.Combine(absoluteDir, filename);

                /// 文件不存在
                if (absoluteDirFiles.Contains(absoluteFile) is false)
                {
                    Console.Log("LocaleFileFetch", "NO-FOUND {0}", parsedUrl.Path);
                    var notFoundResponse = new PureResponse(HttpStatusCode.NotFound, Body: new PureUtf8StringBody(string.Format("not found file: {0}.", parsedUrl.Path)));
                    return notFoundResponse;
                }

                /// 开始读取文件来响应内容

                //var okResponse = new HttpResponseMessage(HttpStatusCode.OK);
                var fs = File.OpenRead(absoluteFile);
                Console.Log("LocaleFileFetch", "Mode: {0}", mode);

                PureBody responseBody;
                var ipcHeaders = new IpcHeaders()
                    .Set("Content-Length", fs.Length.ToString())
                    .Set("Content-Type", GetMimeType(filename));

                // buffer 模式，就是直接全部读取出来
                // TODO auto 模式就是在通讯次数和单次通讯延迟之间的一个取舍，如果分片次数少于2次，那么就直接发送，没必要分片
                if (mode is "stream")
                {
                    /// 返回流
                    responseBody = new PureStreamBody(fs);
                }
                else using (fs)
                    {
                        /// 一次性发送
                        responseBody = new PureByteArrayBody(await fs.ReadBytesAsync(fs.Length));
                    }

                return new PureResponse(HttpStatusCode.OK, ipcHeaders, responseBody, Url: request.Url);
            }
        }
        catch (Exception e)
        {
            Console.Warn("LocaleFileFetch", "Exception: {0}", e.Message);
            return new PureResponse(HttpStatusCode.InternalServerError);
        }

        return null;
    }
}

