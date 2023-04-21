
namespace DwebBrowser.MicroService.Core;

using FetchAdapter = Func<MicroModule, HttpRequestMessage, Task<HttpResponseMessage?>>;

public class NativeFetch
{
	public NativeFetch()
	{
	}

	public static AdapterManager<FetchAdapter> NativeFetchAdaptersManager = new();
}

public abstract partial class MicroModule
{
    public async Task<HttpResponseMessage> NativeFetchAsync(HttpRequestMessage request)
    {
        foreach (var fetchAdapter in NativeFetch.NativeFetchAdaptersManager.Adapters)
        {
            var response = await fetchAdapter(this, request);

            if (response is not null)
            {
                return response;
            }
        }

        return await new HttpClient().SendAsync(request);
    }

    public Task<HttpResponseMessage> NativeFetchAsync(Uri url) =>
        NativeFetchAsync(new HttpRequestMessage(HttpMethod.Get, url));

    public Task<HttpResponseMessage> NativeFetchAsync(string url) =>
        NativeFetchAsync(new HttpRequestMessage(HttpMethod.Get, new Uri(url)));
}