
namespace ipc;

public class IpcStreamAbort : IpcMessage
{
    [JsonPropertyName("type")]
    public override IPC_MESSAGE_TYPE Type { get; set; } = IPC_MESSAGE_TYPE.STREAM_ABORT;

    [JsonPropertyName("stream_id")]
    public string StreamId { get; set; }

    public IpcStreamAbort(string stream_id)
    {
        StreamId = stream_id;
    }

    /// <summary>
    /// Serialize IpcStreamAbort
    /// </summary>
    /// <returns>JSON string representation of the IpcStreamAbort</returns>
    public string ToJson()
    {
        return JsonSerializer.Serialize(this);
    }

    /// <summary>
    /// Deserialize IpcStreamAbort
    /// </summary>
    /// <param name="json">JSON string representation of IpcStreamAbort</param>
    /// <returns>An instance of a IpcStreamAbort object.</returns>
    public static IpcStreamAbort? FromJson(string json)
    {
        return JsonSerializer.Deserialize<IpcStreamAbort>(json);
    }
}

