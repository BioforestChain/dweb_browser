
namespace ipc;

public abstract class IpcMessage
{
    [JsonPropertyName("type")]
    public abstract IPC_MESSAGE_TYPE Type { get; set; }
}
