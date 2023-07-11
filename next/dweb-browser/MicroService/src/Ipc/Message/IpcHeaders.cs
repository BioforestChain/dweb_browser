using System.Collections;
using System.Globalization;
using System.Net.Http.Headers;

namespace DwebBrowser.MicroService.Message;

[JsonConverter(typeof(IpcHeadersConverter))]
public class IpcHeaders : IEnumerable<KeyValuePair<string, string>>
{
    static readonly TextInfo textInfo = new CultureInfo("en-US", false).TextInfo;
    public static string NormalizeKey(string key) => textInfo.ToTitleCase(key.ToLower());

    private Dictionary<string, string> HeadersMap { get; set; }

    public IpcHeaders()
    {
        HeadersMap = new Dictionary<string, string>();
    }

    public IpcHeaders(HttpHeaders headers)
    {
        HeadersMap = headers.ToDictionary(h => NormalizeKey(h.Key), h => string.Join(",", h.Value));
    }
    public IpcHeaders(IEnumerable<(string, string)> headers)
    {
        HeadersMap = headers.ToDictionary(h => NormalizeKey(h.Item1), h => h.Item2);
    }
    public IpcHeaders(IEnumerable<KeyValuePair<string, string>> headers)
    {
        HeadersMap = headers.ToDictionary(h => NormalizeKey(h.Key), h => h.Value);
    }
    public IpcHeaders(HttpHeaders headers, HttpContentHeaders? contentHeaders)
    {
        HeadersMap = headers.ToDictionary(h => h.Key, h => h.Value.FirstOrDefault() ?? "");
        if (contentHeaders is not null)
        {
            foreach (var (key, value) in contentHeaders)
            {
                HeadersMap.TryAdd(key, string.Join(",", value));
            }
        }
    }

    public static IpcHeaders With(Dictionary<string, string> headers) => new() { HeadersMap = headers };


    public IpcHeaders Set(string key, string value)
    {
        HeadersMap.Add(NormalizeKey(key), value);
        return this;
    }

    public IpcHeaders Init(string key, string value)
    {
        key = NormalizeKey(key);
        if (!HeadersMap.ContainsKey(key))
        {
            HeadersMap.Add(key, value);
        }
        return this;
    }

    public string? Get(string key) => HeadersMap.GetValueOrDefault(NormalizeKey(key));

    public string GetOrDefault(string key, string defaultValue) => HeadersMap.GetValueOrDefault(NormalizeKey(key)) ?? defaultValue;

    public bool Has(string key) => HeadersMap.ContainsKey(NormalizeKey(key));

    public bool Delete(string key) => HeadersMap.Remove(NormalizeKey(key));

    public void ForEach(Action<string, string> fn)
    {
        foreach (KeyValuePair<string, string> entry in HeadersMap)
        {
            fn(entry.Key, entry.Value);
        }
    }

    /// <summary>
    /// Serialize IpcHeaders
    /// </summary>
    /// <returns>JSON string representation of the IpcHeaders</returns>
    public string ToJson() => JsonSerializer.Serialize(this);

    /// <summary>
    /// Deserialize IpcHeaders
    /// </summary>
    /// <param name="json">JSON string representation of IpcHeaders</param>
    /// <returns>An instance of a IpcHeaders object.</returns>
    public static IpcHeaders? FromJson(string json) => JsonSerializer.Deserialize<IpcHeaders>(json);

    public IEnumerator<KeyValuePair<string, string>> GetEnumerator()
    {
        return HeadersMap.GetEnumerator();
    }
    public Dictionary<string, string> ToDictionary()
    {
        return HeadersMap.ToDictionary(k => k.Key, v => v.Value);
    }

    IEnumerator<KeyValuePair<string, string>> IEnumerable<KeyValuePair<string, string>>.GetEnumerator() => GetEnumerator();

    IEnumerator IEnumerable.GetEnumerator() => GetEnumerator();
}

#region IpcHeaders序列化反序列化
sealed class IpcHeadersConverter : JsonConverter<IpcHeaders>
{
    public override bool CanConvert(Type typeToConvert) =>
        typeToConvert.GetMethod("ToJson") is not null && typeToConvert.GetMethod("FromJson") is not null;

    public override IpcHeaders? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        if (reader.TokenType != JsonTokenType.StartObject)
            throw new JsonException("Expected StartObject token");

        var headers = new Dictionary<string, string>();

        while (reader.Read())
        {
            if (reader.TokenType == JsonTokenType.EndObject)
            {
                return IpcHeaders.With(headers);
            }


            if (reader.TokenType != JsonTokenType.PropertyName)
                throw new JsonException("Expected PropertyName token");

            var key = reader.GetString();

            reader.Read();

            if (key != "")
            {
                headers.Add(key!, reader.GetString() ?? "");
            }
        }

        throw new JsonException("Expected EndObject token");
    }

    public override void Write(Utf8JsonWriter writer, IpcHeaders value, JsonSerializerOptions options)
    {
        writer.WriteStartObject();

        foreach (var entry in value)
        {
            writer.WriteString(entry.Key, entry.Value);
        }

        writer.WriteEndObject();
    }
}
#endregion


static public class IDictionaryExtensions
{
    public static IpcHeaders ToIpcHeaders(this IEnumerable<KeyValuePair<string, string>> allHeaders)
    {
        return new IpcHeaders(allHeaders);
    }
    public static void WriteToHttpMessage(this IEnumerable<KeyValuePair<string, string>> allHeaders, HttpHeaders httpHeaders, HttpContentHeaders? contentHeaders)
    {
        if (contentHeaders is null)
        {
            foreach (var (key, value) in allHeaders)
            {
                switch (key)
                {
                    case "Content-Encoding":
                    case "Content-Length":
                    case "Content-Language":
                    case "Content-Location":
                    case "Content-Disposition":
                    case "Content-MD5":
                    case "Content-Range":
                    case "Content-Type":
                        break;
                    default:
                        httpHeaders.TryAddWithoutValidation(key, value);
                        break;
                }
            }
            return;
        }
        foreach (var (key, value) in allHeaders)
        {
            switch (key)
            {
                case "Content-Encoding":
                    contentHeaders.ContentEncoding.Clear();
                    contentHeaders.ContentEncoding.Add(value);
                    break;
                case "Content-Length":
                    contentHeaders.ContentLength = value.ToLongOrNull();
                    break;
                case "Content-Language":
                    contentHeaders.ContentLanguage.Clear();
                    contentHeaders.ContentLanguage.Add(value);
                    break;
                case "Content-Location":
                    if (Uri.TryCreate(value, new(), out var uri))
                    {
                        contentHeaders.ContentLocation = uri;
                    }
                    break;
                case "Content-Disposition":
                    if (ContentDispositionHeaderValue.TryParse(value, out var contentDisposition))
                    {
                        contentHeaders.ContentDisposition = contentDisposition;
                    }
                    break;
                case "Content-MD5":
                    contentHeaders.ContentMD5 = value.ToBase64ByteArray();
                    break;
                case "Content-Range":
                    if (ContentRangeHeaderValue.TryParse(value, out var contentRange))
                    {
                        contentHeaders.ContentRange = contentRange;
                    }
                    break;
                case "Content-Type":
                    if (MediaTypeHeaderValue.TryParse(value, out var contentType))
                    {
                        contentHeaders.ContentType = contentType;
                    }
                    break;
                default:
                    httpHeaders.TryAddWithoutValidation(key, value);
                    break;
            }

        }
    }

}