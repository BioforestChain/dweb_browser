using UIKit;

namespace DwebBrowser.MicroService.Sys.Mwebview;

public class MultiWebViewController : UIViewController
{
    public Mmid Mmid { get; set; }
    public MultiWebViewNMM LocaleMM { get; set; }
    public MicroModule RemoteMM { get; set; }

    public MultiWebViewController(Mmid mmid, MultiWebViewNMM localeMM, MicroModule remoteMM)
    {
        Mmid = mmid;
        LocaleMM = localeMM;
        RemoteMM = remoteMM;
    }

    private static int s_webviewId_acc = 1;

    public record ViewItem(string webviewId, DWebView.DWebView webView);

    
}

