using UIKit;
using DwebBrowser.MicroService.Sys.Js;

namespace DwebBrowser.Platforms.iOS;

public class Program
{
    
    static Debugger Console = new Debugger("Program");
    // This is the main entry point of the application.
    static void Main(string[] args)
    {

        Console.Log("Main", "Start");
        _ = MicroService.Start();
        // if you want to use a different Application Delegate class from "AppDelegate"
        // you can specify it here.
        UIApplication.Main(args, null, typeof(AppDelegate));
        Console.Log("Main", "End");
    }
}
