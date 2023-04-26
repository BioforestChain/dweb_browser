using System.Net.Http;

namespace DwebBrowser.MicroService.Message;

public class IpcRequest : IpcMessage
{
    public int ReqId { get; init; }
    public IpcMethod Method { get; init; }
    public string Url { get; init; }
    public IpcHeaders Headers { get; set; }
    public IpcBody Body { get; set; }
    public Ipc Ipc { get; init; }

    public IpcRequest(int req_id, string url, IpcMethod method, IpcHeaders headers, IpcBody body, Ipc ipc) : base(IPC_MESSAGE_TYPE.REQUEST)
    {
        ReqId = req_id;
        Url = url;
        Method = method;
        Headers = headers;
        Body = body;
        Ipc = ipc;

        Uri = new Uri(url);
    }

    public Uri Uri { get; init; }

    public static IpcRequest FromText(int req_id, string url, IpcMethod method, IpcHeaders headers, string text, Ipc ipc) =>
        new IpcRequest(req_id, url, method ?? IpcMethod.Get, headers ?? new IpcHeaders(), IpcBodySender.FromText(text, ipc), ipc);

    public static IpcRequest FromBinary(int req_id, string url, IpcMethod method, IpcHeaders headers, byte[] binary, Ipc ipc) =>
        new IpcRequest(
            req_id,
            url,
            method,
            (headers ??= new IpcHeaders()).Also(it =>
            {
                it.Init("Content-Type", "application/octet-stream");
                it.Init("Content-Length", binary.Length.ToString());
            }),
            IpcBodySender.FromBinary(binary, ipc),
            ipc);

    public static IpcRequest FromStream(
        int req_id,
        string url,
        IpcMethod method,
        IpcHeaders headers,
        Stream stream,
        Ipc ipc,
        long? size
        ) => new IpcRequest(
                req_id,
                url,
                method,
                (headers ??= new IpcHeaders()).Also(it =>
                {
                    it.Init("Content-Type", "application/octet-stream");

                    if (size != null)
                    {
                        headers.Init("Content-Length", size.ToString()!);
                    }
                }),
                IpcBodySender.FromStream(stream, ipc),
                ipc);

    public static async Task<IpcRequest> FromRequest(int req_id, HttpRequestMessage request, Ipc ipc)
    {
        var body = (request.Method.Method is "GET" or "HEAD")
                ? IpcBodySender.FromText("", ipc)
                : request.Content switch
                {
                    StringContent stringContent => IpcBodySender.FromText(await stringContent.ReadAsStringAsync(), ipc),
                    ByteArrayContent byteArrayContent => IpcBodySender.FromBinary(await byteArrayContent.ReadAsByteArrayAsync(), ipc),
                    StreamContent streamContent => IpcBodySender.FromStream(await streamContent.ReadAsStreamAsync(), ipc),
                    null => IpcBodySender.FromText("", ipc),
                    _ => await request.Content.ReadAsStreamAsync().Let(async streamTask =>
                    {
                        var stream = await streamTask;
                        try
                        {
                            if (stream.Length == 0)
                            {
                                return IpcBodySender.FromText("", ipc);
                            }
                        }
                        catch
                        { // ignore error
                        }
                        return IpcBodySender.FromStream(stream, ipc);
                    })

                };

        var ipcRequest = new IpcRequest(req_id,
                request.RequestUri?.ToString() ?? "",
                IpcMethod.From(request.Method),
                new IpcHeaders(request.Headers, request.Content?.Headers),
                body,
                ipc
            );
        return ipcRequest;
    }


    public HttpRequestMessage ToRequest() =>
        new HttpRequestMessage(new HttpMethod(Method.Method), new Uri(Url)).Also(it =>
            {
                switch (Body.Raw)
                {
                    case string body:
                        it.Content = new StringContent(body);
                        break;
                    case byte[] body:
                        it.Content = new ByteArrayContent(body);
                        break;
                    case Stream body:
                        it.Content = new StreamContent(body);
                        break;
                    default:
                        throw new Exception(String.Format("invalid body to request: {0}", Body.Raw));
                }

                foreach (var (key, value) in Headers.GetEnumerator())
                {
                    if (key.StartsWith("Content-", true, null))
                    {
                        it.Content.Headers.Add(key, value);
                    }
                    else
                    {
                        it.Headers.TryAddWithoutValidation(key, value);
                    }
                }
            });

    public IpcReqMessage LazyIpcReqMessage
    {
        get
        {
            return new Lazy<IpcReqMessage>(new Func<IpcReqMessage>(() =>
                new IpcReqMessage(
                    ReqId,
                    Method,
                    Url,
                    Headers.GetEnumerator().ToDictionary(k => k.Key, v => v.Value),
                    Body.MetaBody)), true).Value;
        }
    }

    public override string ToString() => String.Format("#IpcRequest/{0}/{1}", Method.Method, Url);
}

public class IpcReqMessage : IpcMessage
{
    [Obsolete("使用带参数的构造函数", true)]
    public IpcReqMessage() : base(IPC_MESSAGE_TYPE.REQUEST)
    {
        /// 给JSON反序列化用的空参数构造函数
    }

    [JsonPropertyName("req_id")]
    public int ReqId { get; set; }
    [JsonPropertyName("method")]
    public IpcMethod Method { get; set; }
    [JsonPropertyName("url")]
    public string Url { get; set; }
    [JsonPropertyName("headers")]
    public Dictionary<string, string> Headers { get; set; }
    [JsonPropertyName("metaBody")]
    public MetaBody MetaBody { get; set; }

    public IpcReqMessage(
        int req_id,
        IpcMethod method,
        string url,
        Dictionary<String, String> headers,
        MetaBody metaBody) : base(IPC_MESSAGE_TYPE.REQUEST)
    {
        ReqId = req_id;
        Method = method;
        Url = url;
        Headers = headers;
        MetaBody = metaBody;
    }

    public IpcReqMessage JsonAble() => MetaBody.JsonAble().Let(metaBody =>
    {
        if (metaBody != MetaBody)
        {
            return new IpcReqMessage(ReqId, Method, Url, Headers, metaBody);
        }
        return this;
    });

    /// <summary>
    /// Serialize IpcReqMessage
    /// </summary>
    /// <returns>JSON string representation of the IpcReqMessage</returns>
    public override string ToJson() => JsonSerializer.Serialize(JsonAble(), new JsonSerializerOptions { IncludeFields = true });

    /// <summary>
    /// Deserialize IpcReqMessage
    /// </summary>
    /// <param name="json">JSON string representation of IpcReqMessage</param>
    /// <returns>An instance of a IpcReqMessage object.</returns>
    public static new IpcReqMessage? FromJson(string json) => JsonSerializer.Deserialize<IpcReqMessage>(json, new JsonSerializerOptions { IncludeFields = true });
}
