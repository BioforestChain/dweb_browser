using System.Text.Json;
using DwebBrowser.MicroService.Browser.Mwebview;
using Foundation;
using UIKit;

#nullable enable

namespace DwebBrowser.MicroService.Sys.Share;

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
            var formData = await request.ReadFromDataAsync();

            var stream = new ReadableStream(onStart: async (controller) =>
            {
                var result = await ShowSharePanel(
                ipc!,
                new ShareOptions
                {
                    Title = title,
                    Text = text,
                    Url = url,
                    Files = formData.Files.Select(file =>
                    {
                        /// TODO 我们需要将文件暂时存储起来吧？
                        return file.FileName;
                    }).ToArray()
                });
                await controller.EnqueueAsync(JsonSerializer.Serialize(new ShareResult(result is "OK", result)).ToUtf8ByteArray());
                controller.Close();
            });

            return stream.Stream;
        });
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
            if (options.Files is not null && options.Files.Length > 0)
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
    public string[]? Files { get; set; }
}

public sealed record ShareResult(bool success, string message);