using System.Net.Http.Headers;
using System.Reflection.PortableExecutable;

namespace DwebBrowser.MicroService.Message;

[JsonConverter(typeof(IpcHeadersConverter))]
public class IpcHeaders
{
    private Dictionary<string, string> _headersMap { get; set; }

    public IpcHeaders()
    {
        _headersMap = new Dictionary<string, string>();
    }

    public IpcHeaders(HttpHeaders headers)
    {
        _headersMap = headers.ToDictionary(h => h.Key, h => h.Value.FirstOrDefault() ?? "");
    }
    public IpcHeaders(HttpHeaders headers, HttpContentHeaders? contentHeaders)
    {
        _headersMap = headers.ToDictionary(h => h.Key, h => h.Value.FirstOrDefault() ?? "");
        if (contentHeaders is not null)
        {
            foreach (var (key, value) in contentHeaders)
            {
                _headersMap.TryAdd(key, string.Join(",", value));
            }
        }
    }

    public static IpcHeaders With(Dictionary<string, string> headers) => new IpcHeaders() { _headersMap = headers };


    public void Set(string key, string value) => _headersMap.Add(key.ToLower(), value);

    public void Init(string key, string value)
    {
        if (!_headersMap.ContainsKey(key))
        {
            _headersMap.Add(key.ToLower(), value);
        }
    }

    public string? Get(string key) => _headersMap.GetValueOrDefault(key.ToLower());

    public string GetOrDefault(string key, string defaultValue) => _headersMap.GetValueOrDefault(key.ToLower()) ?? defaultValue;

    public bool Has(string key) => _headersMap.ContainsKey(key.ToLower());

    public void Delete(string key) => _headersMap.Remove(key.ToLower());

    public void ForEach(Action<string, string> fn)
    {
        foreach (KeyValuePair<string, string> entry in _headersMap)
        {
            fn(entry.Key, entry.Value);
        }
    }

    public IEnumerable<KeyValuePair<string, string>> GetEnumerator()
    {
        foreach (KeyValuePair<string, string> entry in _headersMap)
        {
            yield return entry;
        }
    }
    public void ToHttpMessage(HttpHeaders headers, HttpContentHeaders contentHeaders)
    {
        foreach (var (key, value) in GetEnumerator())
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
                    headers.TryAddWithoutValidation(key, value);
                    break;
            }

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

        foreach (KeyValuePair<string, string> entry in value.GetEnumerator())
        {
            writer.WriteString(entry.Key, entry.Value);
        }

        writer.WriteEndObject();
    }
}
#endregion

