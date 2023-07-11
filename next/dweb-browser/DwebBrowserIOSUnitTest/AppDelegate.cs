using DwebBrowser.MicroService.Browser;
using DwebBrowser.MicroService.Browser.Jmm;
using DwebBrowser.MicroService.Browser.JsProcess;
//using System.Diagnostics;
using System.Net.Http;
using DwebBrowserIOSUnitTest.Tests;
using DwebBrowser.Helper;
using DwebBrowser.MicroService.Message;

namespace DwebBrowserIOSUnitTest;

[Register("AppDelegate")]
public class AppDelegate : UIApplicationDelegate
{
    public override UIWindow? Window
    {
        get;
        set;
    }

    public override bool FinishedLaunching(UIApplication application, NSDictionary launchOptions)
    {
        // create a new window instance based on the screen size
        Window = new UIWindow(UIScreen.MainScreen.Bounds);

        //ColorTest.FromRgba_string_ReturnSuccess();
        //UTTypesTest.UTTypes_ToString();

        Debugger.DebugTags = new() { "*" };
        // create a UIViewController with a single UILabel
        var vc = new UIViewController();
        //vc.View.Frame = UIScreen.MainScreen.Bounds;
        Window.RootViewController = vc;

        // make the window visible
        Window.MakeKeyAndVisible();


        Task.Run(() =>
        {
            var x = System.Diagnostics.Stopwatch.StartNew();


            foreach (var j in Enumerable.Range(1, 10))
            {

                var str = "";
                foreach (var i in Enumerable.Range(j, 10))
                {
                    str += i.ToString();
                }
                var data = new MetaBody(MetaBody.IPC_META_BODY_TYPE.INLINE_BASE64, j, str, "r" + j, 3 + j);
                x.Reset();
                x.Start();
                System.Diagnostics.Debug.WriteLine("Size: {0}, Time: {1}", data, x.ElapsedMilliseconds);
                var json = data.ToJson();
                x.Stop();
                System.Diagnostics.Debug.WriteLine("Size: {0}, Time: {1}", json.Length, x.ElapsedMilliseconds);
            }


            foreach (var j in Enumerable.Range(1, 10))
            {

                var str = "";
                foreach (var i in Enumerable.Range(j, 60000))
                {
                    str += i.ToString();
                }
                var data = new MetaBody(MetaBody.IPC_META_BODY_TYPE.INLINE_BASE64, j, str, "r"+j, 3+j);
                x.Reset();
                x.Start();
                System.Diagnostics.Debug.WriteLine("Size: {0}, Time: {1}", data, x.ElapsedMilliseconds);
                var json = data.ToJson();
                x.Stop();
                System.Diagnostics.Debug.WriteLine("Size: {0}, Time: {1}", json.Length, x.ElapsedMilliseconds);
            }
        });
        return true;
    }
}

