
namespace ipc;

[JsonConverter(typeof(IpcStreamPullConverter))]
public class IpcStreamPull : IpcMessage, IpcStream
{
    //[JsonPropertyName("type")]
    public override IPC_MESSAGE_TYPE Type { get; set; } = IPC_MESSAGE_TYPE.STREAM_PULL;

    //[JsonPropertyName("stream_id")]
    public string StreamId { get; set; }

    //[JsonPropertyName("desiredSize")]
    public int DesiredSize { get; set; }

    public IpcStreamPull(string stream_id, int desiredSize = 1)
    {
        StreamId = stream_id;
        DesiredSize = desiredSize;
    }

    internal IpcStreamPull() { }

    /// <summary>
    /// Serialize IpcStreamPull
    /// </summary>
    /// <returns>JSON string representation of the IpcStreamPull</returns>
    public override string ToJson() => JsonSerializer.Serialize(this);

    /// <summary>
    /// Deserialize IpcStreamPull
    /// </summary>
    /// <param name="json">JSON string representation of IpcStreamPull</param>
    /// <returns>An instance of a IpcStreamPull object.</returns>
    public static IpcStreamPull? FromJson(string json) => JsonSerializer.Deserialize<IpcStreamPull>(json);
}

#region IpcStreamPull序列化反序列化
sealed class IpcStreamPullConverter : JsonConverter<IpcStreamPull>
{
    public override bool CanConvert(Type typeToConvert) =>
        typeToConvert.GetMethod("ToJson") != null && typeToConvert.GetMethod("FromJson") != null;


    public override IpcStreamPull? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        if (reader.TokenType != JsonTokenType.StartObject)
            throw new Exception("Expected StartObject token");

        var ipcStreamPull = new IpcStreamPull();

        while (reader.Read())
        {
            if (reader.TokenType == JsonTokenType.EndObject)
                return ipcStreamPull;

            if (reader.TokenType != JsonTokenType.PropertyName)
                throw new Exception("Expected PropertyName token");

            var propName = reader.GetString();

            reader.Read();

            switch (propName)
            {
                case "type":
                    ipcStreamPull.Type = (IPC_MESSAGE_TYPE)reader.GetInt16();
                    break;
                case "stream_id":
                    ipcStreamPull.StreamId = reader.GetString() ?? "";
                    break;
                case "desiredSize":
                    ipcStreamPull.DesiredSize = reader.GetInt32();
                    break;
            }
        }

        throw new JsonException("Expected EndObject token");
    }

    public override void Write(Utf8JsonWriter writer, IpcStreamPull value, JsonSerializerOptions options)
    {
        writer.WriteStartObject();

        writer.WriteNumber("type", (int)value.Type);
        writer.WriteString("stream_id", value.StreamId);
        writer.WriteNumber("desiredSize", value.DesiredSize);

        writer.WriteEndObject();
    }
}
#endregion