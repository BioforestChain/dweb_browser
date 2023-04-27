using DwebBrowser.MicroService.Sys.Jmm;
namespace DwebBrowser.MicroService.Sys.User;

public class CotDemoJMM: JsMicroModule
{
	public CotDemoJMM(): base(new JmmMetadata(
		"cotdemo.bfs.dweb",
		new JmmMetadata.MainServer() { Root = "file:///bundle", Entry = "/cotDemo.worker.js" },
		splashScreen: new JmmMetadata.SSplashScreen("https://www.bfmeta.org/"),
		staticWebServers: new List<JmmMetadata.StaticWebServer>
		{
			new JmmMetadata.StaticWebServer("file:///bundle", "/cotDemo.worker.js", "cotdemo.bfs.dweb", 80)
		}))
	{
		JmmNMM.GetAndUpdateJmmNmmApps().Add(Mmid, this);
	}
}

