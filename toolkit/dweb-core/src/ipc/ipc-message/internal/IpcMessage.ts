export const enum IPC_MESSAGE_TYPE {
  /**生命周期 */
  LIFECYCLE = "life",
  /** 类型：请求 */
  REQUEST = "req",
  /** 类型：相应 */
  RESPONSE = "res",
  /** 类型：流数据，发送方 */
  STREAM_DATA = "data",
  /** 类型：流拉取，请求方
   * 发送方一旦收到该指令，就可以持续发送数据
   * 该指令中可以携带一些“限流协议信息”，如果违背该协议，请求方可能会断开连接
   */
  STREAM_PULLING = "pull",
  /** 类型：流暂停，请求方
   * 发送方一旦收到该指令，就应当停止基本的数据发送
   * 该指令中可以携带一些“保险协议信息”，描述仍然允许发送的一些数据类型、发送频率等。如果违背该协议，请求方可以会断开连接
   */
  STREAM_PAUSED = "pause",
  /** 类型：流关闭，发送方
   * 可能是发送完成了，也有可能是被中断了
   */
  STREAM_END = "end",
  /** 类型：流中断，请求方 */
  STREAM_ABORT = "abo",
  /** 类型：事件 */
  EVENT = "event",
  /**错误响应 */
  ERROR = "err",
  /**类型：分叉*/
  FORK = "fork",
}

export const ipcMessageBase = <T extends IPC_MESSAGE_TYPE>(type: T) => ({ type } as const);
