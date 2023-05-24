import { $Callback, createSignal } from "../../helper/createSignal.js";
import { PromiseOut } from "../../helper/PromiseOut.js";
import { binaryStreamRead } from "../../helper/readableStreamHelper.js";
import { IPC_MESSAGE_TYPE } from "./const.js";
import type { Ipc } from "./ipc.js";
import { BodyHub, IpcBody, type $BodyData } from "./IpcBody.js";
import { IpcStreamData } from "./IpcStreamData.js";
import { IpcStreamEnd } from "./IpcStreamEnd.js";
import type { IpcStreamPaused } from "./IpcStreamPaused.js";
import type { IpcStreamPulling } from "./IpcStreamPulling.js";
import { IPC_META_BODY_TYPE, MetaBody } from "./MetaBody.js";

/**
 * 控制信号
 */
enum STREAM_CTOR_SIGNAL {
  PULLING,
  PAUSED,
  ABORTED,
}
type $UsedIpcInfo = InstanceType<IpcBodySender["UsedIpcInfo"]>;

export class IpcBodySender extends IpcBody {
  static from(data: $BodyData, ipc: Ipc) {
    if (typeof data !== "string") {
      const cache = IpcBody.wm.get(data);
      if (cache !== undefined) {
        return cache;
      }
    }
    return new IpcBodySender(data, ipc);
  }
  constructor(readonly data: $BodyData, private readonly ipc: Ipc) {
    super();
    if (typeof data !== "string") {
      IpcBody.wm.set(data, this);
    }
    /// 作为 "生产者"，第一持有这个 IpcBodySender
    IpcBodySender.$usableByIpc(ipc, this);
  }

  readonly isStream = this.data instanceof ReadableStream;

  private streamCtorSignal =
    createSignal<(signal: STREAM_CTOR_SIGNAL) => unknown>();

  /**
   * 被哪些 ipc 所真正使用，使用的进度分别是多少
   *
   * 这个进度 用于 类似流的 多发
   */
  private readonly usedIpcMap = new Map<Ipc, $UsedIpcInfo>();
  private UsedIpcInfo = class UsedIpcInfo {
    constructor(
      readonly ipcBody: IpcBodySender,
      readonly ipc: Ipc,
      public bandwidth = 0,
      public fuse = 0
    ) {}
    emitStreamPull(message: IpcStreamPulling) {
      return this.ipcBody.emitStreamPull(this, message);
    }

    emitStreamPaused(message: IpcStreamPaused) {
      return this.ipcBody.emitStreamPaused(this, message);
    }

    emitStreamAborted() {
      return this.ipcBody.emitStreamAborted(this);
    }
  };

  /**
   * 绑定使用
   */
  private useByIpc(ipc: Ipc) {
    const info = this.usedIpcMap.get(ipc);
    if (info !== undefined) {
      return info;
    }
    /// 如果是未开启的流，插入
    if (this.isStream && !this._isStreamOpened) {
      const info = new this.UsedIpcInfo(this, ipc);
      this.usedIpcMap.set(ipc, info);
      this.closeSignal.listen(() => {
        this.emitStreamAborted(info);
      });
      return info;
    }
  }
  /**
   * 拉取数据
   */
  private emitStreamPull(info: $UsedIpcInfo, message: IpcStreamPulling) {
    /// desiredSize 仅作参考，我们以发过来的拉取次数为准
    info.bandwidth = message.bandwidth;
    // 只要有一个开始读取，那么就可以开始
    this.streamCtorSignal.emit(STREAM_CTOR_SIGNAL.PULLING);
  }
  /**
   * 暂停数据
   */
  private emitStreamPaused(info: $UsedIpcInfo, message: IpcStreamPaused) {
    /// 更新保险限制
    info.bandwidth = -1;
    info.fuse = message.fuse;

    /// 如果所有的读取者都暂停了，那么就触发暂停
    let paused = true;
    for (const info of this.usedIpcMap.values()) {
      if (info.bandwidth >= 0) {
        paused = false;
        break;
      }
    }
    if (paused) {
      this.streamCtorSignal.emit(STREAM_CTOR_SIGNAL.PAUSED);
    }
  }

  /**
   * 解绑使用
   */
  private emitStreamAborted(info: $UsedIpcInfo) {
    if (this.usedIpcMap.delete(info.ipc) != null) {
      /// 如果没有任何消费者了，那么真正意义上触发 abort
      if (this.usedIpcMap.size === 0) {
        this.streamCtorSignal.emit(STREAM_CTOR_SIGNAL.ABORTED);
      }
    }
  }

  private readonly closeSignal = createSignal();
  onStreamClose(cb: $Callback) {
    return this.closeSignal.listen(cb);
  }

  private readonly openSignal = createSignal();
  onStreamOpen(cb: $Callback) {
    return this.openSignal.listen(cb);
  }

  private _isStreamOpened = false;
  public get isStreamOpened() {
    return this._isStreamOpened;
  }
  public set isStreamOpened(value) {
    if (this._isStreamOpened !== value) {
      this._isStreamOpened = value;
      if (value) {
        this.openSignal.emit();
        this.openSignal.clear();
      }
    }
  }
  private _isStreamClosed = false;
  public get isStreamClosed() {
    return this._isStreamClosed;
  }
  public set isStreamClosed(value) {
    if (this._isStreamClosed !== value) {
      this._isStreamClosed = value;
      if (value) {
        this.closeSignal.emit();
        this.closeSignal.clear();
      }
    }
  }
  private emitStreamClose() {
    this.isStreamOpened = true;
    this.isStreamClosed = true;
  }

  /// bodyAsMeta

  protected _bodyHub = new BodyHub(this.data);
  readonly metaBody = this.$bodyAsMeta(this.data, this.ipc);

  private $bodyAsMeta(body: $BodyData, ipc: Ipc): MetaBody {
    if (typeof body === "string") {
      return MetaBody.fromText(ipc.uid, body);
    }
    if (body instanceof ReadableStream) {
      return this.$streamAsMeta(body, ipc);
    }
    return MetaBody.fromBinary(ipc, body);
  }
  /**
   * 如果 rawData 是流模式，需要提供数据发送服务
   *
   * 这里不会一直无脑发，而是对方有需要的时候才发
   * @param stream_id
   * @param stream
   * @param ipc
   */
  private $streamAsMeta(
    stream: ReadableStream<Uint8Array>,
    ipc: Ipc
  ): MetaBody {
    const stream_id = getStreamId(stream);
    const reader = binaryStreamRead(stream);
    (async () => {
      /**
       * 流的使用锁(Future 锁)
       * 只有等到 Pulling 指令的时候才能读取并发送
       */
      let pullingLock = new PromiseOut<void>();
      this.streamCtorSignal.listen(async (signal) => {
        switch (signal) {
          case STREAM_CTOR_SIGNAL.PULLING: {
            pullingLock.resolve();
            break;
          }
          case STREAM_CTOR_SIGNAL.PAUSED: {
            if (pullingLock.is_finished) {
              pullingLock = new PromiseOut();
            }
            break;
          }
          case STREAM_CTOR_SIGNAL.ABORTED: {
            /// stream 现在在 locked 状态，binaryStreamRead 的 reutrn 可以释放它的 locked
            await reader.return();
            /// 然后取消流的读取
            await stream.cancel();
            this.emitStreamClose();
          }
        }
      });

      /// 持续发送数据
      while (true) {
        // 等待流开始被拉取
        await pullingLock.promise;

        // const desiredSize = this.maxPulledSize - this.curPulledSize;
        const availableLen = await reader.available();
        if (availableLen > 0) {
          // 开光了，流已经开始被读取
          this.isStreamOpened = true;

          const message = IpcStreamData.fromBinary(
            stream_id,
            await reader.readBinary(availableLen)
          );
          for (const ipc of this.usedIpcMap.keys()) {
            ipc.postMessage(message);
          }
        } else if (availableLen === -1) {
          /// 不论是不是被 aborted，都发送结束信号
          const message = new IpcStreamEnd(stream_id);
          for (const ipc of this.usedIpcMap.keys()) {
            ipc.postMessage(message);
          }

          this.emitStreamClose();
          break;
        }
      }
    })().catch(console.error);

    let streamType = IPC_META_BODY_TYPE.STREAM_ID;
    let streamFirstData: string | Uint8Array = "";
    if (
      "preReadableSize" in stream &&
      typeof stream.preReadableSize === "number" &&
      stream.preReadableSize > 0
    ) {
      // js的不支持输出预读取帧
    }

    return new MetaBody(streamType, ipc.uid, streamFirstData, stream_id);
  }

  /**
   * ipc 将会使用它
   */
  static $usableByIpc = (ipc: Ipc, ipcBody: IpcBodySender) => {
    if (ipcBody.isStream && !ipcBody._isStreamOpened) {
      const streamId = ipcBody.metaBody.streamId!;
      let usableIpcBodyMapper = IpcUsableIpcBodyMap.get(ipc);
      if (usableIpcBodyMapper === undefined) {
        const mapper = new UsableIpcBodyMapper();
        mapper.onDestroy(
          ipc.onStream((message) => {
            switch (message.type) {
              case IPC_MESSAGE_TYPE.STREAM_PULLING:
                mapper
                  .get(message.stream_id)
                  ?.useByIpc(ipc)
                  ?.emitStreamPull(message);
                break;
              case IPC_MESSAGE_TYPE.STREAM_PAUSED:
                mapper
                  .get(message.stream_id)
                  ?.useByIpc(ipc)
                  ?.emitStreamPaused(message);
                break;
              case IPC_MESSAGE_TYPE.STREAM_ABORT:
                mapper
                  .get(message.stream_id)
                  ?.useByIpc(ipc)
                  ?.emitStreamAborted();
                break;
            }
          })
        );
        mapper.onDestroy(() => IpcUsableIpcBodyMap.delete(ipc));
        usableIpcBodyMapper = mapper;
      }
      if (usableIpcBodyMapper.add(streamId, ipcBody)) {
        // 一个流一旦关闭，那么就将不再会与它有主动通讯上的可能
        ipcBody.onStreamClose(() => usableIpcBodyMapper!.remove(streamId));
      }
    }
  };
}
const streamIdWM = new WeakMap<ReadableStream<Uint8Array>, string>();
let stream_id_acc = 0;
const getStreamId = (stream: ReadableStream<Uint8Array>) => {
  let id = streamIdWM.get(stream);
  if (id === undefined) {
    id = `rs-${stream_id_acc++}`;
    streamIdWM.set(stream, id);
  }
  return id;
};
export const setStreamId = (
  stream: ReadableStream<Uint8Array>,
  cid: string
) => {
  let id = streamIdWM.get(stream);
  if (id === undefined) {
    streamIdWM.set(stream, (id = `rs-${stream_id_acc++}[${cid}]`));
  }
  return id;
};

class UsableIpcBodyMapper {
  private map = new Map<String, IpcBodySender>();
  add(streamId: string, ipcBody: IpcBodySender) {
    if (this.map.has(streamId)) {
      return true;
    }
    this.map.set(streamId, ipcBody);
    return false;
  }

  get(streamId: string) {
    return this.map.get(streamId);
  }
  remove(streamId: string) {
    const ipcBody = this.map.get(streamId);
    if (ipcBody !== undefined) {
      this.map.delete(streamId);
      /// 如果都删除完了，那么就触发事件解绑
      if (this.map.size === 0) {
        this.destroySignal.emit();
        this.destroySignal.clear();
      }
    }
  }
  private destroySignal = createSignal();
  onDestroy(cb: $Callback) {
    this.destroySignal.listen(cb);
  }
}

const IpcUsableIpcBodyMap = new WeakMap<Ipc, UsableIpcBodyMapper>();

/**
 * 可预读取的流
 */
interface PreReadableInputStream {
  /**
   * 对标 InputStream.available 函数
   * 返回可预读的数据
   */
  preReadableSize: number;
}
