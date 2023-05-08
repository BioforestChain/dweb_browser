
using DwebBrowser.MicroService.Http;

namespace DwebBrowser.MicroService.Core;

using FetchAdapter = Func<MicroModule, PureRequest, Task<PureResponse?>>;

public class NativeFetch
{
    public NativeFetch()
    {
    }

    public static AdapterManager<FetchAdapter> NativeFetchAdaptersManager = new();
}

public abstract partial class MicroModule
{
    public async Task<PureResponse> NativeFetchAsync(PureRequest pureRequest)
    {
        foreach (var fetchAdapter in NativeFetch.NativeFetchAdaptersManager.Adapters)
        {
            var response = await fetchAdapter(this, pureRequest);

            if (response is not null)
            {
                return response;
            }
        }

        var httpResponse = await new HttpClient().SendAsync(pureRequest.ToHttpRequestMessage());
        return await httpResponse.ToPureResponseAsync();
    }
    public Task<PureResponse> NativeFetchAsync(URL url) =>
        NativeFetchAsync(new PureRequest(url.Href, IpcMethod.Get));

    public Task<PureResponse> NativeFetchAsync(Uri url) =>
        NativeFetchAsync(new PureRequest(url.ToString(), IpcMethod.Get));

    public Task<PureResponse> NativeFetchAsync(string url) =>
        NativeFetchAsync(new PureRequest(url, IpcMethod.Get));
}