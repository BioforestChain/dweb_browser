namespace ipc;

public class IpcRequest : IpcMessage
{
    public int ReqId { get; init; }
    public override IPC_MESSAGE_TYPE Type { get; set; } = IPC_MESSAGE_TYPE.REQUEST;
    public IpcMethod Method { get; init; }
    public string Url { get; init; }
    public IpcHeaders Headers { get; set; }
    public IpcBody Body { get; set; }
    public Ipc Ipc { get; init; }

    public IpcRequest(int req_id, string url, IpcMethod method, IpcHeaders headers, IpcBody body, Ipc ipc)
    {
        ReqId = req_id;
        Url = url;
        Method = method;
        Headers = headers;
        Body = body;
        Ipc = ipc;
    }

    public static IpcRequest FromText(int req_id, string url, IpcMethod method, IpcHeaders headers, string text, Ipc ipc) =>
        new IpcRequest(req_id, url, method ?? IpcMethod.Get, headers ?? new IpcHeaders(), IpcBodySender.From(text, ipc), ipc);

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
            IpcBodySender.From(binary, ipc),
            ipc);

    public static IpcRequest FromStream(
        int req_id,
        string url,
        IpcMethod method,
        IpcHeaders headers,
        Stream stream,
        Ipc ipc,
        Int64? size
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
                IpcBodySender.From(stream, ipc),
                ipc);

    public static IpcRequest FromRequest(int req_id, HttpRequestMessage request, Ipc ipc) =>
        new IpcRequest(
            req_id,
            request.RequestUri!.ToString(),
            IpcMethod.From(request.Method),
            new IpcHeaders(request.Headers),
            (request.Method.Method is "GET" or "HEAD")
                ? IpcBodySender.From("", ipc)
                :
            request.Content == null
                ? IpcBodySender.From("", ipc)
                : request.Content!.ReadAsStream().Let(it => it.Length switch
                    {
                        0L => IpcBodySender.From("", ipc),
                        _ => IpcBodySender.From(it, ipc)
                    }
                ),
            ipc
            );


    public HttpRequestMessage ToRequest() =>
        new HttpRequestMessage(new HttpMethod(Method.method), new Uri(Url)).Also(it =>
            {
                switch (Body.Raw)
                {
                    case string body:
                        it.Content = new StringContent(body);
                        break;
                    case byte[] body:
                        it.Content = new StreamContent(new MemoryStream().Let(s =>
                        {
                            s.Write(body, 0, body.Length);
                            return s;
                        }));
                        break;
                    case Stream body:
                        it.Content = new StreamContent(body);
                        break;
                    default:
                        throw new Exception($"invalid body to request: {Body.Raw}");
                }

                foreach (KeyValuePair<string, string> entry in Headers.GetEnumerator())
                {
                    it.Content.Headers.Add(entry.Key, entry.Value);
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

    public override string ToString() => $"#IpcRequest/{Method.method}/{Url}";
}

[JsonConverter(typeof(IpcReqMessageConverter))]
public class IpcReqMessage : IpcMessage
{
    public override IPC_MESSAGE_TYPE Type { get; set; } = IPC_MESSAGE_TYPE.REQUEST;

    public int ReqId { get; set; }
    public IpcMethod Method { get; set; }
    public string Url { get; set; }
    public Dictionary<string, string> Headers { get; set; }
    public SMetaBody MetaBody { get; set; }

    public IpcReqMessage(
        int req_id,
        IpcMethod method,
        string url,
        Dictionary<String, String> headers,
        SMetaBody metaBody)
    {
        ReqId = req_id;
        Method = method;
        Url = url;
        Headers = headers;
        MetaBody = metaBody;
    }

    /// <summary>
    /// Serialize IpcReqMessage
    /// </summary>
    /// <returns>JSON string representation of the IpcReqMessage</returns>
    public override string ToJson() => JsonSerializer.Serialize(this);

    /// <summary>
    /// Deserialize IpcReqMessage
    /// </summary>
    /// <param name="json">JSON string representation of IpcReqMessage</param>
    /// <returns>An instance of a IpcReqMessage object.</returns>
    public static IpcReqMessage? FromJson(string json) => JsonSerializer.Deserialize<IpcReqMessage>(json);
}

#region IpcReqMessage序列化反序列化
sealed class IpcReqMessageConverter : JsonConverter<IpcReqMessage>
{
    public override bool CanConvert(Type typeToConvert) =>
        typeToConvert.GetMethod("ToJson") != null && typeToConvert.GetMethod("FromJson") != null;


    public override IpcReqMessage? Read(
        ref Utf8JsonReader reader,
        Type typeToConvert,
        JsonSerializerOptions options)
    {
        if (reader.TokenType != JsonTokenType.StartObject)
            throw new JsonException("Expected StartObject token");

        int req_id = default;
        IPC_MESSAGE_TYPE type = default;
        string url = default;
        IpcMethod method = default;
        SMetaBody metaBody = default;
        var headers = new Dictionary<string, string>();
        while (reader.Read())
        {
            if (reader.TokenType == JsonTokenType.EndObject)
                return new IpcReqMessage(req_id, method ?? IpcMethod.Get, url ?? "", headers, metaBody);

            if (reader.TokenType != JsonTokenType.PropertyName)
                throw new JsonException("Expected PropertyName token");

            var propName = reader.GetString();

            reader.Read();

            switch (propName)
            {
                case "req_id":
                    req_id = reader.GetInt32();
                    break;
                case "type":
                    type = (IPC_MESSAGE_TYPE)reader.GetInt16();
                    break;
                case "url":
                    url = reader.GetString() ?? "";
                    break;
                case "method":
                    method = new IpcMethod(reader.GetString() ?? "GET");
                    break;
                case "metaBody":
                    metaBody = (SMetaBody)SMetaBody.FromJson(reader.GetString()!)!;
                    break;
                case "headers":
                    while (reader.Read())
                    {
                        if (reader.TokenType == JsonTokenType.StartObject)
                        {
                            continue;
                        }

                        if (reader.TokenType == JsonTokenType.EndObject)
                        {
                            break;
                        }

                        var memberName = reader.GetString();

                        reader.Read();

                        if (memberName != null)
                        {
                            headers.Add(memberName, reader.GetString() ?? "");
                        }
                    }

                    break;
            }
        }

        throw new JsonException("Expected EndObject token");
    }

    public override void Write(
        Utf8JsonWriter writer,
        IpcReqMessage value,
        JsonSerializerOptions options)
    {
        writer.WriteStartObject();

        writer.WriteNumber("req_id", value.ReqId);
        writer.WriteNumber("type", (int)value.Type);
        writer.WriteString("method", value.Method.method);
        writer.WriteString("url", value.Url);
        writer.WriteString("metaBody", value.MetaBody.ToJson());

        // dictionary
        writer.WritePropertyName("headers");
        writer.WriteStartObject();

        foreach ((string key, string keyValue) in value.Headers)
        {

            writer.WriteString(key, keyValue);
        }

        writer.WriteEndObject();

        writer.WriteEndObject();
    }
}
#endregion
