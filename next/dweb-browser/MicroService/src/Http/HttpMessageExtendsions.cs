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
            _ => content.ToString() switch
            {
                "System.Net.Http.EmptyContent" => PureBody.Empty,
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
            }
        };
    }
    static public HttpContent? ToHttpContent(this PureBody pureBody)
    {
        return pureBody switch
        {
            PureUtf8StringBody stringBody => new StringContent(stringBody.Data),
            PureByteArrayBody byteArrayBody => new ByteArrayContent(byteArrayBody.Data),
            PureStreamBody streamBody => new StreamContent(streamBody.Data),
            PureEmptyBody => null,
            var body => new StreamContent(body.ToStream())
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
    public static PureRequest ToPureRequest(this HttpListenerRequest self)
    {
        // Create a new HttpRequestMessage object
        var pureRequest = new PureRequest(
            // Set the request URI
            self.Url?.AbsoluteUri ?? "",
            // Set the request method
            IpcMethod.From(self.HttpMethod),
            new IpcHeaders().Also(ipcHeaders =>
            {
                // Copy the headers from the HttpListenerRequest to the HttpRequestMessage
                foreach (var key in self.Headers.AllKeys)
                {
                    if (key is not null && self.Headers.Get(key) is not null and var value)
                        ipcHeaders.Set(key, value);
                }
                /// 这里理论上是有包含 Content-Headers 的
            }),
           self.HasEntityBody switch
           {
               true => new PureStreamBody(self.InputStream),
               false => null
           }
         );

        return pureRequest;
    }
    static public HttpRequestMessage ToHttpRequestMessage(this PureRequest self)
    {
        var repuest = new HttpRequestMessage(self.Method.ToHttpMethod(), self.Url);

        self.Body.ToHttpContent()?.Also(content => repuest.Content = content);

        self.Headers.WriteToHttpMessage(repuest.Headers, repuest.Content?.Headers);
        return repuest;
    }

    static public async Task<PureResponse> ToPureResponseAsync(this HttpResponseMessage response)
    {
        var body = await response.Content.ToPureBody();

        var ipcResponse = new PureResponse(
                response.StatusCode,
                new IpcHeaders(response.Headers, response.Content?.Headers),
                body,
                response.ReasonPhrase,
                response.RequestMessage?.RequestUri?.ToString()
            );
        return ipcResponse;
    }


    static public HttpResponseMessage ToHttpResponseMessage(this PureResponse self)
    {
        var response = new HttpResponseMessage(self.StatusCode);
        response.ReasonPhrase = self.StatusText;

        self.Body.ToHttpContent()?.Also(content => response.Content = content);

        self.Headers.WriteToHttpMessage(response.Headers, response.Content?.Headers);
        return response;
    }

    public static async Task<HttpListenerResponse> WriteToHttpListenerResponse(this PureResponse self, HttpListenerResponse response)
    {
        response.StatusCode = (int)self.StatusCode;

        foreach (var header in self.Headers)
        {
            response.Headers[header.Key] = string.Join(",", header.Value);
        }

        //response.ContentEncoding = self.Headers.Get("Content-Encoding");
        //response.ContentType = self.Headers.Get("Content-Type");
        //response.ContentLength64 = self.Headers.Get("Content-Length")?.ToLongOrNull() ?? 0;

        switch (self.Body)
        {
            case PureEmptyBody:
                break;
            case PureStreamBody streamBody:
                await streamBody.Data.CopyToAsync(response.OutputStream);
                break;
            //case PureByteArrayBody byteArrayBody:
            //case PureUtf8StringBody byteArrayBody:
            default:
                await response.OutputStream.WriteAsync(self.Body.ToByteArray());
                break;
        }

        return response;
    }
}