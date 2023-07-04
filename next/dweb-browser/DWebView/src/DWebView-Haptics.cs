using DwebBrowser.Helper;
using DwebBrowser.MicroService.Core;
using WebKit;

namespace DwebBrowser.DWebView;

public partial class DWebView : WKWebView
{
    private LazyBox<WKScriptMessageHandler> _webHapticsMessageHandler = new();
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
            var message = messageEvent.Body;
            var path = (string)(NSString)message.ValueForKey(new NSString("path"));
            var style = (string)(NSString)message.ValueForKey(new NSString("style"));
            var duration = (string)(NSString)message.ValueForKey(new NSString("duration"));

            if (path is not null)
            {
                string? urlString;

                if (duration is not null)
                {
                    urlString = string.Format("file://haptics.browser.dweb/{0}?duration={1}", path, duration);
                }
                else if (style is not null)
                {
                    urlString = string.Format("file://haptics.browser.dweb/{0}?style={1}", path, style);
                }
                else
                {
                    urlString = string.Format("file://haptics.browser.dweb/{0}", path);
                }

                await LocaleMM.NativeFetchAsync(urlString);
            }
        }
    }
}