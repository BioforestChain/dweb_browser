import { $Callback, createSignal } from "../../helper/createSignal.cjs";
import { binaryStreamRead } from "../../helper/readableStreamHelper.cjs";
import type { Ipc } from "./ipc.cjs";
import { BodyHub, IpcBody, type $BodyData } from "./IpcBody.cjs";
import { IpcStreamAbort } from "./IpcStreamAbort.cjs";
import { IpcStreamData } from "./IpcStreamData.cjs";
import { IpcStreamEnd } from "./IpcStreamEnd.cjs";
import { IpcStreamPull } from "./IpcStreamPull.cjs";
import { IPC_META_BODY_TYPE, MetaBody } from "./MetaBody.cjs";

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

  private pullSignal = createSignal();
  private abortSignal = createSignal();

  /**
   * 被哪些 ipc 所真正使用，使用的进度分别是多少
   *
   * 这个进度 用于 类似流的 多发
   */
  private readonly usedIpcMap = new Map<Ipc, /* PulledSize */ number>();

  /**
   * 当前收到拉取的请求数
   */
  private curPulledTimes = 0;

  /**
   * 绑定使用
   */
  private useByIpc(ipc: Ipc) {
    if (this.usedIpcMap.has(ipc)) {
      return true;
    }
    if (this.isStream && !this._isStreamOpened) {
      this.usedIpcMap.set(ipc, 0);
      this.closeSignal.listen(() => {
        this.unuseByIpc(ipc);
      });
      return true;
    }
    return false;
  }
  /**
   * 拉取数据
   */
  private emitStreamPull(message: IpcStreamPull, ipc: Ipc) {
    /// desiredSize 仅作参考，我们以发过来的拉取次数为准
    const pulledSize = this.usedIpcMap.get(ipc)! + message.desiredSize;
    this.usedIpcMap.set(ipc, pulledSize);
    this.pullSignal.emit();
  }

  /**
   * 解绑使用
   */
  private unuseByIpc(ipc: Ipc) {
    if (this.usedIpcMap.delete(ipc) != null) {
      /// 如果没有任何消费者了，那么真正意义上触发 abort
      if (this.usedIpcMap.size === 0) {
        this.abortSignal.emit();
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

    const sender = async () => {
      /// 如果原本就不为0，那么就说明已经在运行中了
      if (this.curPulledTimes++ > 0) {
        return;
      }
      /// 读满预期值
      while (this.curPulledTimes > 0) {
        // const desiredSize = this.maxPulledSize - this.curPulledSize;
        const availableLen = await reader.available();
        switch (availableLen) {
          case -1:
          case 0:
            {
              /// 不论是不是被 aborted，都发送结束信号
              const message = new IpcStreamEnd(stream_id);
              for (const ipc of this.usedIpcMap.keys()) {
                ipc.postMessage(message);
              }

              this.emitStreamClose();
            }
            break;
          default: {
            // 开光了，流已经开始被读取
            this.isStreamOpened = true;

            const data = await reader.readBinary(availableLen);
            const message = IpcStreamData.fromBinary(stream_id, data);
            for (const ipc of this.usedIpcMap.keys()) {
              ipc.postMessage(message);
            }
          }
        }

        /// 只要发送过一次，那么就把所有请求指控，根据协议，我能发多少是多少，你不够的话，再来要
        this.curPulledTimes = 0;
      }
    };
    this.pullSignal.listen(() => {
      void sender();
    });
    this.abortSignal.listen(() => {
      reader.return();
      this.emitStreamClose();
    });

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
          ipc.onMessage((message) => {
            if (message instanceof IpcStreamPull) {
              const ipcBody = mapper.get(message.stream_id);
              // 一个流一旦开启了，那么就无法再被外部使用了
              if (ipcBody?.useByIpc(ipc)) {
                // ipc 将使用这个 body，也就是说接下来的 MessageData 也要通知一份给这个 ipc
                ipcBody.emitStreamPull(message, ipc);
              }
            } else if (message instanceof IpcStreamAbort) {
              const ipcBody = mapper.get(message.stream_id);
              ipcBody?.unuseByIpc(ipc);
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
