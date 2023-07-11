

namespace DwebBrowser.MicroService.Message;

[JsonConverter(typeof(IpcMethodConverter))]
public class IpcMethod
{
    [JsonPropertyName("method")]
    private readonly string _method;

    public static readonly IpcMethod Get = new("GET");

    public static readonly IpcMethod Put = new("PUT");

    public static readonly IpcMethod Post = new("POST");

    public static readonly IpcMethod Delete = new("DELETE");

    public static readonly IpcMethod Head = new("HEAD");

    public static readonly IpcMethod Options = new("OPTIONS");

    public static readonly IpcMethod Trace = new("TRACE");

    public static readonly IpcMethod Patch = new("PATCH");

    public static readonly IpcMethod Connect = new("CONNECT");

    public string Method => _method;

    private IpcMethod(string method)
    {
        _method = method;
    }

    public static IpcMethod From(HttpMethod method) => From(method.Method);
    public static IpcMethod From(string method) => method switch
    {
        "GET" => Get,
        "PUT" => Put,
        "POST" => Post,
        "DELETE" => Delete,
        "HEAD" => Head,
        "OPTIONS" => Options,
        "TRACE" => Trace,
        "PATCH" => Patch,
        "CONNECT" => Connect,
        _ => throw new ArgumentException(string.Format("Unknown type {0}", method))
    };

    public override string ToString() => _method;

    private readonly LazyBox<HttpMethod> _httpMethod = new();
    public HttpMethod ToHttpMethod() => _httpMethod.GetOrPut(() => new(_method));

    /// <summary>
    /// Serialize IpcMethod
    /// </summary>
    /// <returns>JSON string representation of the IpcMethod</returns>
    public string ToJson() => JsonSerializer.Serialize(this);

    /// <summary>
    /// Deserialize IpcMethod
    /// </summary>
    /// <param name="json">JSON string representation of IpcMethod</param>
    /// <returns>An instance of a IpcMethod object.</returns>
    public static IpcMethod? FromJson(string json) => JsonSerializer.Deserialize<IpcMethod>(json);
}

#region IpcHeaders序列化反序列化
public class IpcMethodConverter : JsonConverter<IpcMethod>
{
    public override bool CanConvert(Type typeToConvert) =>
        typeToConvert.GetMethod("ToJson") is not null && typeToConvert.GetMethod("FromJson") is not null;

    public override IpcMethod? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        var method = reader.GetString();
        return method switch
        {
            "GET" => IpcMethod.Get,
            "PUT" => IpcMethod.Put,
            "POST" => IpcMethod.Post,
            "DELETE" => IpcMethod.Delete,
            "HEAD" => IpcMethod.Head,
            "OPTIONS" => IpcMethod.Options,
            "TRACE" => IpcMethod.Trace,
            "PATCH" => IpcMethod.Patch,
            "CONNECT" => IpcMethod.Connect,
            _ => throw new JsonException("Invalid Ipc Method Type")
        };

    }

    public override void Write(Utf8JsonWriter writer, IpcMethod value, JsonSerializerOptions options)
    {
        writer.WriteStringValue(value.Method);
    }
}
#endregion

