using System.Net;
using System.Text.Json;
using System.Text.Json.Serialization;
using Foundation;
using UIKit;
using UniformTypeIdentifiers;


namespace DwebBrowser.MicroService.Sys.Clipboard;

public class ClipboardNMM : NativeMicroModule
{
    static readonly Debugger Console = new("ClipboardNMM");

    public ClipboardNMM() : base("clipboard.sys.dweb", "clipboard")
    {
    }

    private readonly UIPasteboard _pasteboard = UIPasteboard.General;

    protected override async Task _bootstrapAsync(IBootstrapContext bootstrapContext)
    {
        HttpRouter.AddRoute(IpcMethod.Get, "/read", async (request, _) =>
        {
            var reader = Read();
            Console.Log("/read", reader);
            return new HttpResponseMessage(HttpStatusCode.OK).Also(it =>
                it.Content = new StringContent(reader));
        });

        HttpRouter.AddRoute(IpcMethod.Get, "/write", async (request, _) =>
        {
            var text = request.QueryString("string");
            var image = request.QueryString("image");
            var url = request.QueryString("url");
            Console.Log("/write", "string: {0}, image: {1}, url: {2}", text, image, url);

            if (text is null && image is null && url is null)
            {
                return new HttpResponseMessage(HttpStatusCode.UnsupportedMediaType);
            }

            if (Write(text, image, url, out var errMessage))
            {
                return new HttpResponseMessage(HttpStatusCode.OK).Also(it =>
                    it.Content = new StringContent("true"));
            }

            Console.Log("/write", "errMessage: {0}", errMessage);

            return false;
        });
    }

    public string Read()
    {
        ClipboardContent clipboardContent;
        if (_pasteboard.HasStrings)
        {
            clipboardContent = new()
            { Value = _pasteboard.String ?? "", Type = "text/plain" };
        }
        else if (_pasteboard.HasImages)
        {
            var base64 = _pasteboard.Image?.AsPNG().GetBase64EncodedString(NSDataBase64EncodingOptions.None);
            clipboardContent = new()
            { Value = "data:image/png;base64," + base64, Type = "text/png" };
        }
        else if (_pasteboard.HasUrls)
        {
            clipboardContent = new()
            { Value = _pasteboard.Url?.AbsoluteString ?? "", Type = "text/plain" };
        }
        else
        {
            clipboardContent = new() { Value = "", Type = "text/plain" };
        }

        return JsonSerializer.Serialize(clipboardContent);
    }

    public bool Write(string? text, string? image, string? url, out string errMessage)
    {
        var _bool = false;
        errMessage = "";
        if (text is not null)
        {
            _pasteboard.String = text;
            _bool = true;
        }
        else if (image is not null)
        {
            image = image.Replace("data:image/png;base64,", "");
            _pasteboard.Image = UIImage.LoadFromData(
                new(image, NSDataBase64DecodingOptions.None));
            _bool = true;
        }
        else if (url is not null)
        {
            _pasteboard.Url = new(url);
            _bool = true;
        }
        else
        {
            errMessage = "no data provided";
        }

        return _bool;
    }
}

public sealed record UIPastboardType(string type)
{
    public static readonly UIPastboardType Text = new(UTTypes.Text.ToString());
    public static readonly UIPastboardType Image = new(UTTypes.Image.ToString());
    public static readonly UIPastboardType Url = new(UTTypes.Url.ToString());
}

public class ClipboardContent
{
    [JsonPropertyName("value")]
    public string Value { get; set; }

    [JsonPropertyName("type")]
    public string Type { get; set; }
}