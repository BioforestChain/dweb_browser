
namespace ipc;

public class IpcStreamPull: IpcMessage
{
    [JsonPropertyName("type")]
    public override IPC_MESSAGE_TYPE Type { get; set; } = IPC_MESSAGE_TYPE.STREAM_PULL;

    [JsonPropertyName("stream_id")]
    public string StreamId { get; init; }

    [JsonPropertyName("desiredSize")]
    public int DesiredSize { get; init; }

    public IpcStreamPull(string stream_id, int desiredSize = 1)
    {
        StreamId = stream_id;
        DesiredSize = desiredSize;
    }

    /// <summary>
    /// Serialize IpcStreamPull
    /// </summary>
    /// <returns>JSON string representation of the IpcStreamPull</returns>
    public string ToJson() => JsonSerializer.Serialize(this);

    /// <summary>
    /// Deserialize IpcStreamPull
    /// </summary>
    /// <param name="json">JSON string representation of IpcStreamPull</param>
    /// <returns>An instance of a IpcStreamPull object.</returns>
    public static IpcStreamPull? FromJson(string json) => JsonSerializer.Deserialize<IpcStreamPull>(json);
}
