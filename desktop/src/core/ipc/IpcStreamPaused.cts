import { IpcMessage, IPC_MESSAGE_TYPE } from "./const.cjs";

export class IpcStreamPaused extends IpcMessage<IPC_MESSAGE_TYPE.STREAM_PAUSED> {
  /**
   * 保险丝次数
   * 虽然协议上暂停了，但保不齐对方有一些特殊的消息需要发过来
   * 此时提供保险丝次数，是用于告知对方违规的次数。
   * 但这个数值不为0的时候，对方还是可以推送一些应用层上特殊的数据过来，协议在应用层中，需要保证这些数据是可以被消化的，
   * 如果推送过来的数据在上层应用层无法被消化，上层可以调用接口消耗一条保险丝：再次发送 更少fuse 的 paused 消息过去。
   * 一旦该数值为0，对方再发送数据过来的时候，底层会直接断开连接。
   */
  readonly fuse;
  constructor(readonly stream_id: string, fuse?: number | null) {
    super(IPC_MESSAGE_TYPE.STREAM_PAUSED);
    this.fuse = fuse ?? 1;
  }
}
