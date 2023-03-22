using Microsoft.Extensions.Logging;
using ipc;
using System.Text.Json;



namespace dweb_browser;

public static class MauiProgram
{
	public static MauiApp CreateMauiApp()
	{
		Console.WriteLine("Maui Start");
        //var json = new IpcReqMessage(
        //	req_id : 0,
        //	method: IpcMethod.Get,
        //	url: "https://www.baidu.com",
        //	headers: new Dictionary<string, string> { { "content-type", "application/json" }, { "encoding", "utf-8" } }
        //).ToJson();
        //      Console.WriteLine(json);
        //Console.WriteLine(IpcReqMessage.FromJson(json));
        //var json = IpcHeaders.With(
        //	new Dictionary<string, string> { { "content-type", "application/json" }, { "encoding", "utf-8" } }
        //).ToJson();
        //      Console.WriteLine(json);
        //Console.WriteLine(IpcHeaders.FromJson(json));

        var json = new IpcResMessage(
            req_id: 0,
            statusCode: 404,
            headers: new Dictionary<string, string> { { "content-type", "application/json" }, { "encoding", "utf-8" } },
            new SMetaBody(SMetaBody.IPC_META_BODY_TYPE.STREAM_ID, 0, "111", "222", 1)
        ).ToJson();
        Console.WriteLine(json);
        Console.WriteLine(IpcResMessage.FromJson(json));
        var builder = MauiApp.CreateBuilder();
		builder
			.UseMauiApp<App>()
			.ConfigureFonts(fonts =>
			{
				fonts.AddFont("OpenSans-Regular.ttf", "OpenSansRegular");
				fonts.AddFont("OpenSans-Semibold.ttf", "OpenSansSemibold");
			});

#if DEBUG
		builder.Logging.AddDebug();
#endif

		return builder.Build();
	}
}

