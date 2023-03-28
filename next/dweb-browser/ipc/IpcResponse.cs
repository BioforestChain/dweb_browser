using System.Net;
namespace ipc;

public class IpcResponse : IpcMessage
{
    public int ReqId { get; init; }
    public override IPC_MESSAGE_TYPE Type { get; set; } = IPC_MESSAGE_TYPE.RESPONSE;
    public int StatusCode { get; init; }
    public IpcHeaders Headers { get; set; }
    public IpcBody Body { get; set; }
    public Ipc ResIpc { get; init; }

    internal IpcResponse()
    { }

    public IpcResponse(int req_id, int statusCode, IpcHeaders headers, IpcBody body, Ipc ipc)
    {
        ReqId = req_id;
        StatusCode = statusCode;
        Headers = headers;
        Body = body;
        ResIpc = ipc;
    }

    // TODO: FromJson 未完成
    public static IpcResponse FromJson(int req_id, int statusCode, IpcHeaders headers, object jsonAble, Ipc ipc) =>
        FromText(req_id, statusCode, headers.Also(it => it.Init("Content-Type", "application/json")), jsonAble.ToString(), ipc);
    public static IpcResponse FromText(int req_id, int statusCode, IpcHeaders headers, string text, Ipc ipc) =>
        new IpcResponse(req_id, statusCode, headers.Also(it => it.Init("Content-Type", "text/plain")), IpcBodySender.From(text, ipc), ipc);

    public static IpcResponse FromBinary(int req_id, int statusCode, IpcHeaders headers, byte[] binary, Ipc ipc) =>
        new IpcResponse(
            req_id,
            statusCode,
            headers.Also(it =>
            {
                it.Init("Content-Type", "application/octet-stream");
                it.Init("Content-Length", binary.Length.ToString());
            }),
            IpcBodySender.From(binary, ipc),
            ipc);

    public static IpcResponse FromStream(int req_id, int statusCode, IpcHeaders headers, Stream stream, Ipc ipc) =>
        new IpcResponse(
            req_id,
            statusCode,
            headers.Also(it => it.Init("Content-Type", "application/octet-stream")),
            IpcBodySender.From(stream, ipc),
            ipc);

    public static IpcResponse FromResponse(int req_id, HttpResponseMessage response, Ipc ipc) =>
        new IpcResponse(
            req_id,
            (int)response.StatusCode,
            new IpcHeaders(response.Headers),
            response.Content.ReadAsStream().Let(it => it.Length switch
            {
                0L => IpcBodySender.From("", ipc),
                _ => IpcBodySender.From(it, ipc)
            }),
            ipc);

    public HttpResponseMessage ToResponse() =>
        new HttpResponseMessage((HttpStatusCode)StatusCode).Also(it =>
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

    public IpcResMessage LazyIpcResMessage
    {
        get
        {
            return new Lazy<IpcResMessage>(new Func<IpcResMessage>(() =>
                new IpcResMessage(
                    ReqId,
                    StatusCode,
                    Headers.GetEnumerator().ToDictionary(k => k.Key, v => v.Value),
                    Body.MetaBody)), true).Value;
        }
    }
}


[JsonConverter(typeof(IpcResMessageConverter))]
public class IpcResMessage : IpcMessage
{
    public override IPC_MESSAGE_TYPE Type { get; set; } = IPC_MESSAGE_TYPE.RESPONSE;

    public int ReqId { get; set; }
    public int StatusCode { get; set; }
    public Dictionary<string, string> Headers { get; set; }
    public SMetaBody MetaBody { get; set; }

    public IpcResMessage(int req_id, int statusCode, Dictionary<string, string> headers, SMetaBody metaBody)
    {
        ReqId = req_id;
        StatusCode = statusCode;
        Headers = headers;
        MetaBody = metaBody;
    }

    internal IpcResMessage()
    { }


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
    public static IpcResMessage? FromJson(string json) => JsonSerializer.Deserialize<IpcResMessage>(json);
}

sealed class IpcResMessageConverter : JsonConverter<IpcResMessage>
{
    public override bool CanConvert(Type typeToConvert) =>
        typeToConvert.GetMethod("ToJson") != null && typeToConvert.GetMethod("FromJson") != null;


    public override IpcResMessage? Read(
        ref Utf8JsonReader reader,
        Type typeToConvert,
        JsonSerializerOptions options)
    {
        if (reader.TokenType != JsonTokenType.StartObject)
            throw new JsonException("Expected StartObject token");

        var ipcResMessage = new IpcResMessage();
        while (reader.Read())
        {
            if (reader.TokenType == JsonTokenType.EndObject)
                return ipcResMessage;

            if (reader.TokenType != JsonTokenType.PropertyName)
                throw new JsonException("Expected PropertyName token");

            var propName = reader.GetString();

            reader.Read();

            switch (propName)
            {
                case "req_id":
                    ipcResMessage.ReqId = reader.GetInt32();
                    break;
                case "type":
                    ipcResMessage.Type = (IPC_MESSAGE_TYPE)reader.GetInt16();
                    break;
                case "statusCode":
                    ipcResMessage.StatusCode = reader.GetInt16();
                    break;
                case "metaBody":
                    ipcResMessage.MetaBody = (SMetaBody)SMetaBody.FromJson(reader.GetString()!)!;
                    break;
                case "headers":
                    var headers = new Dictionary<string, string>();

                    while (reader.Read())
                    {
                        if (reader.TokenType == JsonTokenType.StartObject)
                        {
                            continue;
                        }

                        if (reader.TokenType == JsonTokenType.EndObject)
                        {
                            ipcResMessage.Headers = headers;
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
        IpcResMessage value,
        JsonSerializerOptions options)
    {
        writer.WriteStartObject();

        writer.WriteNumber("req_id", value.ReqId);
        writer.WriteNumber("type", (int)value.Type);
        writer.WriteNumber("statusCode", value.StatusCode);
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
