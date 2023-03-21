using System;


namespace ipc
{
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

        /** 类型：流关闭，发送方
         * 可能是发送完成了，也有可能是被中断了
         */
        STREAM_END = 4,

        /** 类型：流中断，请求方 */
        STREAM_ABORT = 5,

        /** 类型：事件 */
        EVENT = 6,
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
}

