
namespace ipc;

[JsonConverter(typeof(IpcStreamPausedConverter))]
public class IpcStreamPaused : IpcMessage, IpcStream
{
    //[JsonPropertyName("type")]
    public override IPC_MESSAGE_TYPE Type { get; set; } = IPC_MESSAGE_TYPE.STREAM_PAUSED;

    //[JsonPropertyName("stream_id")]
    public string StreamId { get; set; }

    /**
     * 保险丝次数
     * 虽然协议上暂停了，但保不齐对方有一些特殊的消息需要发过来
     * 此时提供保险丝次数，是用于告知对方违规的次数。
     * 但这个数值不为0的时候，对方还是可以推送一些应用层上特殊的数据过来，协议在应用层中，需要保证这些数据是可以被消化的，
     * 如果推送过来的数据在上层应用层无法被消化，上层可以调用接口消耗一条保险丝：再次发送 更少fuse 的 paused 消息过去。
     * 一旦该数值为0，对方再发送数据过来的时候，底层会直接断开连接。
     */
    //[JsonPropertyName("fuse")]
    public int Fuse { get; set; } = 1;

    public IpcStreamPaused(string stream_id)
    {
        StreamId = stream_id;
    }

    internal IpcStreamPaused() { }

    /// <summary>
    /// Serialize IpcStreamPaused
    /// </summary>
    /// <returns>JSON string representation of the IpcStreamPaused</returns>
    public override string ToJson() => JsonSerializer.Serialize(this);

    /// <summary>
    /// Deserialize IpcStreamPaused
    /// </summary>
    /// <param name="json">JSON string representation of IpcStreamPaused</param>
    /// <returns>An instance of a IpcStreamPaused object.</returns>
    public static IpcStreamPaused? FromJson(string json) => JsonSerializer.Deserialize<IpcStreamPaused>(json);
}

sealed class IpcStreamPausedConverter : JsonConverter<IpcStreamPaused>
{
    public override bool CanConvert(Type typeToConvert) =>
        typeToConvert.GetMethod("ToJson") != null && typeToConvert.GetMethod("FromJson") != null;


    public override IpcStreamPaused? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        if (reader.TokenType != JsonTokenType.StartObject)
            throw new Exception("Expected StartObject token");

        var ipcStreamPaused = new IpcStreamPaused();

        while (reader.Read())
        {
            if (reader.TokenType == JsonTokenType.EndObject)
                return ipcStreamPaused;

            if (reader.TokenType != JsonTokenType.PropertyName)
                throw new Exception("Expected PropertyName token");

            var propName = reader.GetString();

            reader.Read();

            switch (propName)
            {
                case "type":
                    ipcStreamPaused.Type = (IPC_MESSAGE_TYPE)reader.GetInt16();
                    break;
                case "stream_id":
                    ipcStreamPaused.StreamId = reader.GetString() ?? "";
                    break;
                case "fuse":
                    ipcStreamPaused.Fuse = reader.GetInt16();
                    break;
            }
        }

        throw new JsonException("Expected EndObject token");
    }

    public override void Write(Utf8JsonWriter writer, IpcStreamPaused value, JsonSerializerOptions options)
    {
        writer.WriteStartObject();

        writer.WriteNumber("type", (int)value.Type);
        writer.WriteString("stream_id", value.StreamId);
        writer.WriteNumber("fuse", value.Fuse);

        writer.WriteEndObject();
    }
}