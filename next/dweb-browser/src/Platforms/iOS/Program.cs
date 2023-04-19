using ObjCRuntime;
using UIKit;
using WebKit;
using DwebBrowser.DWebView;
using Foundation;

namespace DwebBrowser.Platforms.iOS;

public class Program
{
    // This is the main entry point of the application.
    static void Main(string[] args)
    {
        Console.WriteLine("!!!!");
        _ = MicroService.Start();
        // if you want to use a different Application Delegate class from "AppDelegate"
        // you can specify it here.
        UIApplication.Main(args, null, typeof(AppDelegate));
        //var x = new DWebView.DWebView();
        Console.WriteLine("Main End!!!");
    }
}
