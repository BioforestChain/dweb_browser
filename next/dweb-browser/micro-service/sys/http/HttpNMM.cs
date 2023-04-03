using System.Net;
namespace micro_service.sys.http;

public class HttpNMM
{
	public HttpNMM()
	{
	}

	public static Func<HttpRequestMessage, HttpResponseMessage> DefineHandler(
		Func<HttpRequestMessage, object?> handler)
	{
		return request =>
		{
			switch (handler(request))
			{
				case null:
					return new HttpResponseMessage(HttpStatusCode.OK);
				case HttpResponseMessage response:
					return response;
				case byte[] result:
					return new HttpResponseMessage(HttpStatusCode.OK).Also(it =>
					{
						it.Content = new StreamContent(new MemoryStream().Let(s =>
						{
							s.Write(result, 0, result.Length);
							return s;
						}));
					});
				case Stream stream:
					return new HttpResponseMessage(HttpStatusCode.OK).Also(it => it.Content = new StreamContent(stream));
				default:
					return new HttpResponseMessage(HttpStatusCode.OK);
			}
		};
	}

	public static Func<HttpRequestMessage, HttpResponseMessage> DefineHandler(
		Func<HttpRequestMessage, Ipc, object?> handler, Ipc ipc) =>
		DefineHandler(request => handler(request, ipc));
}

