
namespace ipc;

public class IpcStreamEnd : IpcMessage
{
    [JsonPropertyName("type")]
    public override IPC_MESSAGE_TYPE Type { get; set; } = IPC_MESSAGE_TYPE.STREAM_END;

    [JsonPropertyName("stream_id")]
    public string StreamId { get; init; }

    public IpcStreamEnd(string stream_id)
    {
        StreamId = stream_id;
    }

    /// <summary>
    /// Serialize IpcStreamEnd
    /// </summary>
    /// <returns>JSON string representation of the IpcStreamEnd</returns>
    public string ToJson() => JsonSerializer.Serialize(this);

    /// <summary>
    /// Deserialize IpcStreamEnd
    /// </summary>
    /// <param name="json">JSON string representation of IpcStreamEnd</param>
    /// <returns>An instance of a IpcStreamEnd object.</returns>
    public static IpcStreamEnd? FromJson(string json) => JsonSerializer.Deserialize<IpcStreamEnd>(json);
}
