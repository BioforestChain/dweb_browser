using System.Net.Http.Headers;

namespace micro_service.ipc;

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
        typeToConvert.GetMethod("ToJson") != null && typeToConvert.GetMethod("FromJson") != null;

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

