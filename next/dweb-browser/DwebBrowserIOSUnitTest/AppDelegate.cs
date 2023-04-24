using DwebBrowser.WebModule.Jmm;
using DwebBrowser.WebModule.Js;
using System.Net.Http;

namespace DwebBrowserIOSUnitTest;

[Register ("AppDelegate")]
public class AppDelegate : UIApplicationDelegate {
	public override UIWindow? Window {
		get;
		set;
	}

	public override bool FinishedLaunching (UIApplication application, NSDictionary launchOptions)
	{
		// create a new window instance based on the screen size
		Window = new UIWindow (UIScreen.MainScreen.Bounds);

		// create a UIViewController with a single UILabel
		var vc = new UIViewController ();
		vc.View!.AddSubview (new UILabel (Window!.Frame) {
			BackgroundColor = UIColor.SystemBackground,
			TextAlignment = UITextAlignment.Center,
			Text = "Hello, iOS!",
			AutoresizingMask = UIViewAutoresizing.All,
		});
		Window.RootViewController = vc;

		// make the window visible
		Window.MakeKeyAndVisible ();

		var res = LocaleFile.LocaleFileFetch(new JmmNMM(), new HttpRequestMessage(HttpMethod.Get, "file:///bundle/desktop.worker.js"));
		Console.WriteLine(String.Format("结果：{0}", res?.StatusCode));

		Console.WriteLine(res.Content.ToString());

		if (res is not null)
		{
			var stream = res.Content.ReadAsStream();
			Console.WriteLine(String.Format("读取长度：{0}", stream.Length));
			using (var reader = new StreamReader(stream))
			{
				Console.WriteLine(reader.ReadToEnd());
				Console.WriteLine("结束");
			}
		}

		return true;
	}
}

