namespace micro_service.ipc;

public enum IPC_MESSAGE_TYPE : int
{
    /** 类型：请求 */
    REQUEST = 0,

    /** 类型：相应 */
    RESPONSE = 1,

    /** 类型：流数据，发送方 */
    STREAM_DATA = 2,

    /** 类型：流拉取，请求方 */
    STREAM_PULL = 3,

    /** 
     * 类型：推送流，发送方
     * 对方可能没有发送PULL过来，或者发送了被去重了，所以我们需要主动发送PUSH指令，对方收到后，如果状态允许，则会发送PULL指令过来拉取数据
     */
    STREAM_PAUSED = 4,

    /** 类型：流关闭，发送方
        * 可能是发送完成了，也有可能是被中断了
        */
    STREAM_END = 5,

    /** 类型：流中断，请求方 */
    STREAM_ABORT = 6,

    /** 类型：事件 */
    EVENT = 7,
}

/**
 * 可预读取的流
 */
interface IPreReadableInputStream
{
    /**
     * 对标 InputStream.available 函数
     * 返回可预读的数据
     */
    int PreReadableSize { get; set; }
}

[Flags]
public enum IPC_DATA_ENCODING : int
{
    /** UTF8编码的字符串，本质上是 BINARY */
    UTF8 = 1 << 1,

    /** BASE64编码的字符串，本质上是 BINARY */
    BASE64 = 1 << 2,

    /** 二进制, 与 UTF8/BASE64 是对等关系*/
    BINARY = 1 << 3,
}

public enum IPC_ROLE
{
    SERVER,

    CLIENT,
}


