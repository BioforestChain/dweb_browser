using System;
namespace micro_service.sys.dns;

using FetchAdapter = Func<MicroModule, HttpRequestMessage, HttpResponseMessage?>;

public class NativeFetch
{
	public NativeFetch()
	{
	}

	public static AdapterManager<FetchAdapter> NativeFetchAdaptersManager = new();
}

