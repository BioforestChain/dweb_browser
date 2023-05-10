using UIKit;
using System.Text.Json.Serialization;
using DwebBrowser.MicroService.Sys.Mwebview;
using Foundation;
using AngleSharp.Io;
using DwebBrowser.Helper;
using DwebBrowser.MicroService.Http;
using System.Net.Http.Headers;
using Microsoft.AspNetCore.Http;

#nullable enable

namespace DwebBrowser.Platforms.iOS.MicroModule.Plugin.Share;

using FormData = Dictionary<string, List<string>>;

public class ShareNMM : NativeMicroModule
{
    public ShareNMM() : base("share.sys.dweb")
    {
    }

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        /// 系统分享
        HttpRouter.AddRoute(IpcMethod.Post, "/share", async (request, ipc) =>
        {
            var title = request.QueryString("title");
            var text = request.QueryString("text");
            var url = request.QueryString("url");
            var formData = await _parsePostFormDataAsync(request);

            var result = await ShowSharePanel(
                ipc!,
                new ShareOptions
                {
                    Title = title,
                    Text = text,
                    Url = url,
                    Files = formData.GetValueOrDefault("files")
                });

            return new ShareResult(result is "OK", result);
        });
    }

    private async Task<FormData> _parsePostFormDataAsync(PureRequest request)
    {
        var formData = new FormData();
        var stream = request.Body.ToStream();
        var memoryStream = new MemoryStream();
        await stream.CopyToAsync(memoryStream);
        memoryStream.Position = 0;

        // 获取 boundary
        if (MediaTypeHeaderValue.TryParse(request.Headers.Get("Content-Type"), out var contentType) is false)
        {
            throw new Exception("The request could not be parsed properly without a valid Content-Type header.");
        }
        //var x = contentType.Bou

        //var boundary = contentType.Substring(contentType.IndexOf("boundary=") + 9);

        //if (boundary is null)
        //{
        //    throw new HttpRequestException(
        //        "The request could not be parsed properly without a valid Content-Type header." +
        //        " A correct header would include a boundary parameter");
        //}

        //var reader = new StreamReader(memoryStream);

        //string? line = null;
        //while ((line = await reader.ReadLineAsync()) != null)
        //{
        //    if (line == "--" + boundary)
        //    {
        //        // 新部分开始,获取name
        //        var fieldName = reader.ReadLine();
        //        var name = "name=\"";
        //        var start = fieldName.IndexOf(name) + name.Length;
        //        var end = fieldName.IndexOf("\"", start);
        //        var nameString = fieldName.Substring(start, end - start);

        //        // 如果name重复,追加到现有列表
        //        if (formData.TryGetValue(nameString, out var value))
        //        {
        //            value.Add(_readPartData(reader, boundary));
        //        }
        //        else
        //        {
        //            // 新的name,添加列表
        //            formData[nameString] = new List<string> { _readPartData(reader, boundary) };
        //        }
        //    }
        //}

        return formData;
    }

    // 读取part直到下一个boundary
    private string _readPartData(StreamReader reader, string boundary)
    {
        string line;
        string value = "";

        while ((line = reader.ReadLine()) != "--" + boundary)
        {
            value += line + "\r\n";
        }

        return value;
    }

    public Task<string> ShowSharePanel(Ipc ipc, ShareOptions options)
    {
        return MainThread.InvokeOnMainThreadAsync<string>(async () =>
        {
            var items = new List<NSObject>();
            var po = new PromiseOut<string>();

            if (!string.IsNullOrWhiteSpace(options.Title))
            {
                items.Add(new NSString(options.Title));
            }
            if (!string.IsNullOrWhiteSpace(options.Text))
            {
                items.Add(new NSString(options.Text));
            }
            if (!string.IsNullOrWhiteSpace(options.Url))
            {
                items.Add(new NSString(options.Url));
            }
            if (options.Files is not null && options.Files.Count > 0)
            {
                foreach (var fileUrl in options.Files)
                {
                    items.Add(new NSString(fileUrl));
                }
            }

            var activityController = new UIActivityViewController(items.ToArray(), null)
            {
                CompletionWithItemsHandler = (activityType, completed, returnItems, error) =>
                {
                    if (error is not null)
                    {
                        po.Resolve(error.LocalizedDescription);
                        return;
                    }

                    if (completed)
                    {
                        po.Resolve("OK");
                    }
                    else
                    {
                        po.Resolve("Share Canceled");
                    }
                }
            };

            var mwebviewController = MultiWebViewNMM.GetCurrentWebViewController(ipc.Remote.Mmid);
            //if (activityController.PopoverPresentationController is not null && mwebviewController is not null)
            //{
            //    activityController.PopoverPresentationController.SourceView = mwebviewController.webviewContainer;
            //}

            if (mwebviewController is not null)
            {
                if (mwebviewController.PresentedViewController is not null)
                {
                    po.Resolve("Can't share while sharing is in progress");
                }
                else
                {
                    await mwebviewController.PresentViewControllerAsync(activityController, true);
                }
            }
            else
            {
                po.Resolve("No found UIViewController to shown");
            }

            return await po.WaitPromiseAsync();
        });
    }
}

public struct ShareOptions
{
    public string? Title { get; set; }
    public string? Text { get; set; }
    public string? Url { get; set; }
    public List<string>? Files { get; set; }
}

public sealed record ShareResult(bool success, string message);