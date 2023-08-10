using DwebBrowser.Helper;
using DwebBrowser.MicroService.Core;

namespace DwebBrowser.DWebView;

public partial class DWebView : WKWebView
{
    private readonly LazyBox<WKScriptMessageHandler> _webHapticsMessageHandler = new();
    public WKScriptMessageHandler HapticsMessageHanlder
    {
        get => _webHapticsMessageHandler.GetOrPut(() => new WebHapticsMessageHanlder(localeMM));
    }

    internal class WebHapticsMessageHanlder : WKScriptMessageHandler
    {
        MicroModule LocaleMM { get; init; }

        public WebHapticsMessageHanlder(MicroModule localeMM)
        {
            LocaleMM = localeMM;
        }

        [Export("userContentController:didReceiveScriptMessage:")]
        public override async void DidReceiveScriptMessage(WKUserContentController userContentController, WKScriptMessage messageEvent)
        {
            try
            {
                var message = messageEvent.Body;
                var path = (string)(NSString)message.ValueForKey(new NSString("path"));
                var search = message.ValueForKey(new NSString("search"));

                if (path is not null)
                {
                    var urlString = string.Empty;

                    if (search is not null)
                    {
                        var style = (string)(NSString)search.ValueForKey(new NSString("style"));
                        var duration = search.ValueForKey(new NSString("duration"));

                        if (duration is not null)
                        {
                            urlString = string.Format("file://haptics.browser.dweb{0}?duration={1}", path, duration);
                        }
                        else if (style is not null)
                        {
                            urlString = string.Format("file://haptics.browser.dweb{0}?style={1}", path, style);
                        }
                    }
                    else
                    {
                        urlString = string.Format("file://haptics.browser.dweb{0}", path);
                    }

                    await LocaleMM.NativeFetchAsync(urlString);
                }
            } catch { }
        }
    }
}
