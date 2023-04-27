using ObjCRuntime;
using UIKit;
using WebKit;
using DwebBrowser.DWebView;
using Foundation;
using DwebBrowser.MicroService.Sys.Js;

namespace DwebBrowser.Platforms.iOS;

public class Program
{
    static Debugger Console = new Debugger("Program");
    // This is the main entry point of the application.
    static void Main(string[] args)
    {

        MainQueue.Init();
        Console.Log("Main", "Start");
        _ = MicroService.Start();
        // if you want to use a different Application Delegate class from "AppDelegate"
        // you can specify it here.
        UIApplication.Main(args, null, typeof(AppDelegate));
        //var x = new DWebView.DWebView();
        Console.Log("Main", "End");
    }
}
