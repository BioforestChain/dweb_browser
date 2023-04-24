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

    //public static IpcRequest FromRequest(int req_id, HttpRequestMessage request, Ipc ipc) =>
    //    new IpcRequest(
    //        req_id,
    //        request.RequestUri!.ToString(),
    //        IpcMethod.From(request.Method),
    //        new IpcHeaders(request.Headers),
    //        (request.Method.Method is "GET" or "HEAD")
    //            ? IpcBodySender.From("", ipc)
    //            :
    //        request.Content switch
    //        {
    //            null => IpcBodySender.From("", ipc),
    //            _ => IpcBodySender.From(request.Content.ReadAsStream(), ipc)
    //        },
    //        ipc
    //        );
    public static async Task<IpcRequest> FromRequest(int req_id, HttpRequestMessage request, Ipc ipc)
    {

        //switch (self.Content.ToString())
        //{
        //    case "System.Net.Http.StringContent":
        //        var result = JsonSerializer.Deserialize<T>(await self.TextAsync())!;
        //        Console.WriteLine(String.Format("result: {0}", result));
        //        return result;
        //    case "System.IO.MemoryStream":
        //    case "System.Net.Http.StreamContent":
        //        return JsonSerializer.Deserialize<T>(await self.StreamAsync())!;
        //}
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
        Console.WriteLine(request.RequestUri?.ToString());
        try
        {
            var ipcRequest = new IpcRequest(req_id,
                request.RequestUri?.ToString() ?? "",
                IpcMethod.From(request.Method),
                new IpcHeaders(request.Headers),
                body,
                ipc
            );
            return ipcRequest;
        }
        catch (Exception e)
        {
            Console.WriteLine(String.Format("e {0}", e.StackTrace));
            Console.WriteLine(String.Format("e {0}", e.Message));
            throw e;
        }
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

                foreach (var entry in Headers.GetEnumerator())
                {
                    if (entry.Key.StartsWith("Content", true, null))
                    {
                        it.Content.Headers.Add(entry.Key, entry.Value);
                    }
                    else
                    {
                        it.Headers.TryAddWithoutValidation(entry.Key, entry.Value);
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

//[JsonConverter(typeof(IpcReqMessageConverter))]
public class IpcReqMessage : IpcMessage
{
    static void xxx()
    {
        //var x = new IpcReqMessage() {
        //ReqId = 123};
    }

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
    [JsonPropertyName("headers"), JsonConverter(typeof(DictionaryConverter))]
    public Dictionary<string, string> Headers { get; set; }
    [JsonPropertyName("metaBody")]
    public SMetaBody MetaBody { get; set; }

    public IpcReqMessage(
        int req_id,
        IpcMethod method,
        string url,
        Dictionary<String, String> headers,
        SMetaBody metaBody) : base(IPC_MESSAGE_TYPE.REQUEST)
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
    public override string ToJson() => JsonSerializer.Serialize(this, new JsonSerializerOptions { IncludeFields = true });

    /// <summary>
    /// Deserialize IpcReqMessage
    /// </summary>
    /// <param name="json">JSON string representation of IpcReqMessage</param>
    /// <returns>An instance of a IpcReqMessage object.</returns>
    public static new IpcReqMessage? FromJson(string json) => JsonSerializer.Deserialize<IpcReqMessage>(json, new JsonSerializerOptions { IncludeFields = true });
}

//#region IpcReqMessage序列化反序列化
//sealed class IpcReqMessageConverter : JsonConverter<IpcReqMessage>
//{
//    public override bool CanConvert(Type typeToConvert) =>
//        typeToConvert.GetMethod("ToJson") != null && typeToConvert.GetMethod("FromJson") != null;


//    public override IpcReqMessage? Read(
//        ref Utf8JsonReader reader,
//        Type typeToConvert,
//        JsonSerializerOptions options)
//    {
//        if (reader.TokenType != JsonTokenType.StartObject)
//            throw new JsonException("Expected StartObject token");

//        int req_id = default;
//        IPC_MESSAGE_TYPE type = default;
//        string url = default;
//        IpcMethod method = default;
//        SMetaBody metaBody = default;
//        var headers = new Dictionary<string, string>();
//        while (reader.Read())
//        {
//            if (reader.TokenType == JsonTokenType.EndObject)
//                return new IpcReqMessage(req_id, method ?? IpcMethod.Get, url ?? "", headers, metaBody);

//            if (reader.TokenType != JsonTokenType.PropertyName)
//                throw new JsonException("Expected PropertyName token");

//            var propName = reader.GetString();

//            reader.Read();

//            switch (propName)
//            {
//                case "req_id":
//                    req_id = reader.GetInt32();
//                    break;
//                case "type":
//                    type = (IPC_MESSAGE_TYPE)reader.GetInt16();
//                    break;
//                case "url":
//                    url = reader.GetString() ?? "";
//                    break;
//                case "method":
//                    method = new IpcMethod(reader.GetString() ?? "GET");
//                    break;
//                case "metaBody":
//                    SMetaBody.IPC_META_BODY_TYPE mtype = default;
//                    int senderUid = default;
//                    string data = default;
//                    string? stream_id = null;
//                    int? receiverUid = null;
//                    string metaId = default;

//                    while (reader.Read())
//                    {
//                        if (reader.TokenType == JsonTokenType.StartObject)
//                        {
//                            continue;
//                        }

//                        if (reader.TokenType == JsonTokenType.EndObject)
//                        {
//                            metaBody = new SMetaBody(mtype, senderUid, data ?? "", stream_id, receiverUid) { MetaId = metaId ?? "" };
//                            break;
//                        }


//                        if (reader.TokenType != JsonTokenType.PropertyName)
//                            throw new JsonException("Expected PropertyName token");

//                        var mpropName = reader.GetString();

//                        reader.Read();

//                        switch (mpropName)
//                        {
//                            case "type":
//                                mtype = (SMetaBody.IPC_META_BODY_TYPE)reader.GetInt64();
//                                break;
//                            case "senderUid":
//                                senderUid = reader.GetInt32();
//                                break;
//                            case "data":
//                                data = reader.GetString() ?? "";
//                                break;
//                            case "streamId":
//                                stream_id = reader.GetString() ?? null;
//                                break;
//                            case "receiverUid":
//                                receiverUid = reader.GetInt32();
//                                break;
//                            case "metaId":
//                                metaId = reader.GetString() ?? "";
//                                break;
//                        }
//                    }

//                    break;
//                case "headers":
//                    while (reader.Read())
//                    {
//                        if (reader.TokenType == JsonTokenType.StartObject)
//                        {
//                            continue;
//                        }

//                        if (reader.TokenType == JsonTokenType.EndObject)
//                        {
//                            break;
//                        }

//                        var memberName = reader.GetString();

//                        reader.Read();

//                        if (memberName != null)
//                        {
//                            headers.Add(memberName, reader.GetString() ?? "");
//                        }
//                    }

//                    break;
//            }
//        }

//        throw new JsonException("Expected EndObject token");
//    }

//    public override void Write(
//        Utf8JsonWriter writer,
//        IpcReqMessage value,
//        JsonSerializerOptions options)
//    {
//        writer.WriteStartObject();

//        writer.WriteNumber("req_id", value.ReqId);
//        writer.WriteNumber("type", (int)value.Type);
//        writer.WriteString("method", value.Method.method);
//        writer.WriteString("url", value.Url);
//        writer.WriteString("metaBody", value.MetaBody.ToJson());

//        // dictionary
//        writer.WritePropertyName("headers");
//        writer.WriteStartObject();

//        foreach ((string key, string keyValue) in value.Headers)
//        {

//            writer.WriteString(key, keyValue);
//        }

//        writer.WriteEndObject();

//        writer.WriteEndObject();
//    }
//}
//#endregion
