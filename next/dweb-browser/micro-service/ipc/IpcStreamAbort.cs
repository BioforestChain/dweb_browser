
namespace micro_service.ipc;

[JsonConverter(typeof(IpcStreamAbortConverter))]
public class IpcStreamAbort : IpcMessage, IpcStream
{
    public override IPC_MESSAGE_TYPE Type { get; set; } = IPC_MESSAGE_TYPE.STREAM_ABORT;

    public string StreamId { get; set; }

    public IpcStreamAbort(string stream_id)
    {
        StreamId = stream_id;
    }

    /// <summary>
    /// Serialize IpcStreamAbort
    /// </summary>
    /// <returns>JSON string representation of the IpcStreamAbort</returns>
    public override string ToJson() => JsonSerializer.Serialize(this);

    /// <summary>
    /// Deserialize IpcStreamAbort
    /// </summary>
    /// <param name="json">JSON string representation of IpcStreamAbort</param>
    /// <returns>An instance of a IpcStreamAbort object.</returns>
    public static IpcStreamAbort? FromJson(string json) => JsonSerializer.Deserialize<IpcStreamAbort>(json);
}

#region IpcStreamAbort序列化反序列化
sealed class IpcStreamAbortConverter : JsonConverter<IpcStreamAbort>
{
    public override bool CanConvert(Type typeToConvert) =>
        typeToConvert.GetMethod("ToJson") != null && typeToConvert.GetMethod("FromJson") != null;


    public override IpcStreamAbort? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        if (reader.TokenType != JsonTokenType.StartObject)
            throw new Exception("Expected StartObject token");

        IPC_MESSAGE_TYPE type = default;
        string stream_id = default;

        while (reader.Read())
        {
            if (reader.TokenType == JsonTokenType.EndObject)
                return new IpcStreamAbort(stream_id ?? "");

            if (reader.TokenType != JsonTokenType.PropertyName)
                throw new Exception("Expected PropertyName token");

            var propName = reader.GetString();

            reader.Read();

            switch (propName)
            {
                case "type":
                    type = (IPC_MESSAGE_TYPE)reader.GetInt16();
                    break;
                case "stream_id":
                    stream_id = reader.GetString() ?? "";
                    break;
            }
        }

        throw new JsonException("Expected EndObject token");
    }

    public override void Write(Utf8JsonWriter writer, IpcStreamAbort value, JsonSerializerOptions options)
    {
        writer.WriteStartObject();

        writer.WriteNumber("type", (int)value.Type);
        writer.WriteString("stream_id", value.StreamId);

        writer.WriteEndObject();
    }
}
#endregion