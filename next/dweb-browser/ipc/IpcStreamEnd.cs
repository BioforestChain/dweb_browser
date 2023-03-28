
namespace ipc;

[JsonConverter(typeof(IpcStreamEndConverter))]
public class IpcStreamEnd : IpcMessage, IpcStream
{
    //[JsonPropertyName("type")]
    public override IPC_MESSAGE_TYPE Type { get; set; } = IPC_MESSAGE_TYPE.STREAM_END;

    //[JsonPropertyName("stream_id")]
    public string StreamId { get; set; }

    public IpcStreamEnd(string stream_id)
    {
        StreamId = stream_id;
    }

    internal IpcStreamEnd() { }

    /// <summary>
    /// Serialize IpcStreamEnd
    /// </summary>
    /// <returns>JSON string representation of the IpcStreamEnd</returns>
    public override string ToJson() => JsonSerializer.Serialize(this);

    /// <summary>
    /// Deserialize IpcStreamEnd
    /// </summary>
    /// <param name="json">JSON string representation of IpcStreamEnd</param>
    /// <returns>An instance of a IpcStreamEnd object.</returns>
    public static IpcStreamEnd? FromJson(string json) => JsonSerializer.Deserialize<IpcStreamEnd>(json);
}

#region IpcStreamEnd序列化反序列化
sealed class IpcStreamEndConverter : JsonConverter<IpcStreamEnd>
{
    public override bool CanConvert(Type typeToConvert) =>
        typeToConvert.GetMethod("ToJson") != null && typeToConvert.GetMethod("FromJson") != null;


    public override IpcStreamEnd? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        if (reader.TokenType != JsonTokenType.StartObject)
            throw new Exception("Expected StartObject token");

        var ipcStreamEnd = new IpcStreamEnd();

        while (reader.Read())
        {
            if (reader.TokenType == JsonTokenType.EndObject)
                return ipcStreamEnd;

            if (reader.TokenType != JsonTokenType.PropertyName)
                throw new Exception("Expected PropertyName token");

            var propName = reader.GetString();

            reader.Read();

            switch (propName)
            {
                case "type":
                    ipcStreamEnd.Type = (IPC_MESSAGE_TYPE)reader.GetInt16();
                    break;
                case "stream_id":
                    ipcStreamEnd.StreamId = reader.GetString() ?? "";
                    break;
            }
        }

        throw new JsonException("Expected EndObject token");
    }

    public override void Write(Utf8JsonWriter writer, IpcStreamEnd value, JsonSerializerOptions options)
    {
        writer.WriteStartObject();

        writer.WriteNumber("type", (int)value.Type);
        writer.WriteString("stream_id", value.StreamId);

        writer.WriteEndObject();
    }
}
#endregion