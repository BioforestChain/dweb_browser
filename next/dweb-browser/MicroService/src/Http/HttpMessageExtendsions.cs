namespace DwebBrowser.MicroService.Http;


static public class HttpMessageExtendsions
{
    static public async Task<PureBody> ToPureBody(this HttpContent? content)
    {
        return content switch
        {
            StringContent stringContent => new PureUtf8StringBody(await stringContent.ReadAsStringAsync()),
            ByteArrayContent byteArrayContent => new PureByteArrayBody(await byteArrayContent.ReadAsByteArrayAsync()),
            StreamContent streamContent => new PureStreamBody(await streamContent.ReadAsStreamAsync()),
            null => PureBody.Empty,
            _ => await content.ReadAsStreamAsync().Let(async streamTask =>
            {
                PureBody body;
                var stream = await streamTask;
                try
                {
                    if (stream.Length == 0)
                    {
                        body = PureBody.Empty;
                    }
                }
                catch
                { // ignore error
                }
                body = new PureStreamBody(stream);
                return body;
            })
        };
    }
    static public async Task<PureRequest> ToPureRequestAsync(this HttpRequestMessage request)
    {
        var body = (request.Method.Method is "GET" or "HEAD")
               ? PureBody.Empty
               : await request.Content.ToPureBody();

        var ipcRequest = new PureRequest(
                request.RequestUri?.ToString() ?? "",
                IpcMethod.From(request.Method),
                new IpcHeaders(request.Headers, request.Content?.Headers),
                body
            );
        return ipcRequest;
    }
    static public async Task<PureResponse> ToPureResponseAsync(this HttpResponseMessage response)
    {
        var body = await response.Content.ToPureBody();

        var ipcResponse = new PureResponse(
                (int)response.StatusCode,
                new IpcHeaders(response.Headers, response.Content?.Headers),
                body,
                response.ReasonPhrase,
                response.RequestMessage?.RequestUri?.ToString()
            );
        return ipcResponse;
    }
}