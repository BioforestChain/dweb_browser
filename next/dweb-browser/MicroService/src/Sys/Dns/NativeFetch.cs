using System;
using System.Web;
using System.Net;

namespace DwebBrowser.MicroService.Sys.Dns;

using FetchAdapter = Func<MicroModule, HttpRequestMessage, Task<HttpResponseMessage?>>;

public class NativeFetch
{
	public NativeFetch()
	{
	}

	public static AdapterManager<FetchAdapter> NativeFetchAdaptersManager = new();
}