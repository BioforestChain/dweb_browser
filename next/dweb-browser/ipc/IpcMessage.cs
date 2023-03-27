
namespace ipc;

public abstract class IpcMessage
{
    [JsonPropertyName("type")]
    public abstract IPC_MESSAGE_TYPE Type { get; set; }

    /// <summary>
    /// Serialize IpcMessage
    /// </summary>
    /// <returns>JSON string representation of the IpcMessage</returns>
    public virtual string ToJson() => JsonSerializer.Serialize(this);
}

public interface IpcStream
{
    string StreamId { get; set; }
}