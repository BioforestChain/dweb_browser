namespace DwebBrowser.Helper;

public class DictionaryConverter : JsonConverter<Dictionary<string, string>>
{
    public override bool CanConvert(Type typeToConvert) => true;

    public override Dictionary<string, string>? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        if (reader.TokenType != JsonTokenType.StartObject)
            throw new JsonException("Expected StartObject token");

        var headers = new Dictionary<string, string>();

        while (reader.Read())
        {
            if (reader.TokenType == JsonTokenType.EndObject)
            {
                return headers;
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

    public override void Write(Utf8JsonWriter writer, Dictionary<string, string> value, JsonSerializerOptions options)
    {
        writer.WriteStartObject();

        foreach (KeyValuePair<string, string> entry in value)
        {
            writer.WriteString(entry.Key, entry.Value);
        }

        writer.WriteEndObject();
    }
}

