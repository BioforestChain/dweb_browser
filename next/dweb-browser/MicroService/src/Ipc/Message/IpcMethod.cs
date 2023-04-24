

namespace DwebBrowser.MicroService.Message;

[JsonConverter(typeof(IpcMethodConverter))]
public class IpcMethod
{
    private readonly string _method;

    private static readonly IpcMethod s_getMethod = new IpcMethod("GET");

    private static readonly IpcMethod s_putMethod = new IpcMethod("PUT");

    private static readonly IpcMethod s_postMethod = new IpcMethod("POST");

    private static readonly IpcMethod s_deleteMethod = new IpcMethod("DELETE");

    private static readonly IpcMethod s_headMethod = new IpcMethod("HEAD");

    private static readonly IpcMethod s_optionsMethod = new IpcMethod("OPTIONS");

    private static readonly IpcMethod s_traceMethod = new IpcMethod("TRACE");

    private static readonly IpcMethod s_patchMethod = new IpcMethod("PATCH");

    private static readonly IpcMethod s_connectMethod = new IpcMethod("CONNECT");

    public static IpcMethod Get => s_getMethod;

    public static IpcMethod Put => s_putMethod;

    public static IpcMethod Post => s_postMethod;

    public static IpcMethod Delete => s_deleteMethod;

    public static IpcMethod Head => s_headMethod;

    public static IpcMethod Options => s_optionsMethod;

    public static IpcMethod Trace => s_traceMethod;

    public static IpcMethod Patch => s_patchMethod;

    public static IpcMethod Connect => s_connectMethod;

    [JsonPropertyName("method")]
    public string Method => _method;

    public IpcMethod(string method)
    {
        _method = method;
    }

    public static IpcMethod From(HttpMethod method) => new IpcMethod(method.Method);

    public override string ToString() => _method;

    /// <summary>
    /// Serialize IpcMethod
    /// </summary>
    /// <returns>JSON string representation of the IpcMethod</returns>
    public string ToJson() => JsonSerializer.Serialize(this, new JsonSerializerOptions { IncludeFields = true });

    /// <summary>
    /// Deserialize IpcMethod
    /// </summary>
    /// <param name="json">JSON string representation of IpcMethod</param>
    /// <returns>An instance of a IpcMethod object.</returns>
    public static IpcMethod? FromJson(string json) => JsonSerializer.Deserialize<IpcMethod>(json, new JsonSerializerOptions { IncludeFields = true });
}

#region IpcHeaders序列化反序列化
public class IpcMethodConverter : JsonConverter<IpcMethod>
{
    public override bool CanConvert(Type typeToConvert) =>
        typeToConvert.GetMethod("ToJson") != null && typeToConvert.GetMethod("FromJson") != null;

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

