
namespace DwebBrowser.MicroService.Message;

public class IpcStreamPulling : IpcMessage, IpcStream
{

    [JsonPropertyName("stream_id")]
    public string StreamId { get; set; }

    /**
     * 带宽限制, 0 代表不限速。
     * 负数代表暂停，但对于数据暂停，一般使用 Paused 指令。
     * 如果出现负数，往往是代表对方的数据处理出现了阻塞，与 Paused 不同，Paused 代表的是逻辑上的暂停，可能是挂起去处理其它事情去了，
     * 而负数的带宽代表物理意义上的阻塞，此时更不该再发送更多的数据过去
     */
    [JsonPropertyName("bandwidth")]
    public int Bandwidth { get; set; } = 0;

    [Obsolete("使用带参数的构造函数", true)]
    public IpcStreamPulling() : base(IPC_MESSAGE_TYPE.STREAM_PULL)
    {
        /// 给JSON反序列化用的空参数构造函数
    }
    public IpcStreamPulling(string stream_id) : base(IPC_MESSAGE_TYPE.STREAM_PULL)
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
