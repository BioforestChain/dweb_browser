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

        if (body is IpcBodySender ipcBodySender)
        {
            IpcBodySender.IPC.UsableByIpc(ipc, ipcBodySender);
        }
    }

    public Uri Uri { get; init; }

    // 用于判断是否是双工请求 websocket/http3
    public bool IsWebsocketRequest = false;
    public bool IsHttp3Request = false;
    public bool IsDuplex => IsWebsocketRequest || IsHttp3Request;

    public static IpcRequest FromText(int req_id, string url, IpcMethod method, IpcHeaders headers, string text, Ipc ipc) =>
        new(req_id, url, method ?? IpcMethod.Get, headers ?? new IpcHeaders(), IpcBodySender.FromText(text, ipc), ipc);

    public static IpcRequest FromBinary(int req_id, string url, IpcMethod method, IpcHeaders headers, byte[] binary, Ipc ipc) =>
        new(
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
        ) => new(
                req_id,
                url,
                method,
                (headers ??= new IpcHeaders()).Also(it =>
                {
                    it.Init("Content-Type", "application/octet-stream");

                    if (size is not null)
                    {
                        headers.Init("Content-Length", size.ToString()!);
                    }
                }),
                IpcBodySender.FromStream(stream, ipc),
                ipc);


    public IpcReqMessage LazyIpcReqMessage
    {
        get
        {
            return new Lazy<IpcReqMessage>(new Func<IpcReqMessage>(() =>
                new IpcReqMessage(
                    ReqId,
                    Method,
                    Url,
                    Headers.ToDictionary(),
                    Body.MetaBody)), true).Value;
        }
    }

    public override string ToString() => string.Format("#IpcRequest/{0}/{1}", Method.Method, Url);
}

public class IpcReqMessage : IpcMessage
{
    [Obsolete("使用带参数的构造函数", true)]
#pragma warning disable CS8618 // 在退出构造函数时，不可为 null 的字段必须包含非 null 值。请考虑声明为可以为 null。
    public IpcReqMessage() : base(IPC_MESSAGE_TYPE.REQUEST)
#pragma warning restore CS8618 // 在退出构造函数时，不可为 null 的字段必须包含非 null 值。请考虑声明为可以为 null。
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
    public override string ToJson() => JsonSerializer.Serialize(JsonAble());

    /// <summary>
    /// Deserialize IpcReqMessage
    /// </summary>
    /// <param name="json">JSON string representation of IpcReqMessage</param>
    /// <returns>An instance of a IpcReqMessage object.</returns>
    public static new IpcReqMessage? FromJson(string json) => JsonSerializer.Deserialize<IpcReqMessage>(json);
}
