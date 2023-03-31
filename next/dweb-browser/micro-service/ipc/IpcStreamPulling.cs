
namespace micro_service.ipc;

[JsonConverter(typeof(IpcStreamPullingConverter))]
public class IpcStreamPulling : IpcMessage, IpcStream
{
    //[JsonPropertyName("type")]
    public override IPC_MESSAGE_TYPE Type { get; set; } = IPC_MESSAGE_TYPE.STREAM_PULL;

    //[JsonPropertyName("stream_id")]
    public string StreamId { get; set; }

    /**
     * 带宽限制, 0 代表不限速。
     * 负数代表暂停，但对于数据暂停，一般使用 Paused 指令。
     * 如果出现负数，往往是代表对方的数据处理出现了阻塞，与 Paused 不同，Paused 代表的是逻辑上的暂停，可能是挂起去处理其它事情去了，
     * 而负数的带宽代表物理意义上的阻塞，此时更不该再发送更多的数据过去
     */
    //[JsonPropertyName("bandwidth")]
    public int Bandwidth { get; set; } = 0;

    public IpcStreamPulling(string stream_id)
    {
        StreamId = stream_id;
    }

    /// <summary>
    /// Serialize IpcStreamPulling
    /// </summary>
    /// <returns>JSON string representation of the IpcStreamPulling</returns>
    public override string ToJson() => JsonSerializer.Serialize(this);

    /// <summary>
    /// Deserialize IpcStreamPulling
    /// </summary>
    /// <param name="json">JSON string representation of IpcStreamPulling</param>
    /// <returns>An instance of a IpcStreamPulling object.</returns>
    public static IpcStreamPulling? FromJson(string json) => JsonSerializer.Deserialize<IpcStreamPulling>(json);
}

#region IpcStreamPulling序列化反序列化
sealed class IpcStreamPullingConverter : JsonConverter<IpcStreamPulling>
{
    public override bool CanConvert(Type typeToConvert) =>
        typeToConvert.GetMethod("ToJson") != null && typeToConvert.GetMethod("FromJson") != null;


    public override IpcStreamPulling? Read(ref Utf8JsonReader reader, Type typeToConvert, JsonSerializerOptions options)
    {
        if (reader.TokenType != JsonTokenType.StartObject)
            throw new Exception("Expected StartObject token");

        IPC_MESSAGE_TYPE type = default;
        string stream_id = default;
        int bandwidth = default;

        while (reader.Read())
        {
            if (reader.TokenType == JsonTokenType.EndObject)
                return new IpcStreamPulling(stream_id ?? "") { Bandwidth = bandwidth };

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
                case "bandwidth":
                    bandwidth = reader.GetInt16();
                    break;
            }
        }

        throw new JsonException("Expected EndObject token");
    }

    public override void Write(Utf8JsonWriter writer, IpcStreamPulling value, JsonSerializerOptions options)
    {
        writer.WriteStartObject();

        writer.WriteNumber("type", (int)value.Type);
        writer.WriteString("stream_id", value.StreamId);
        writer.WriteNumber("bandwidth", value.Bandwidth);

        writer.WriteEndObject();
    }
}
#endregion