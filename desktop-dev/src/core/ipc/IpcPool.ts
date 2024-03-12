import { $Callback, createSignal } from "../../helper/createSignal.ts";
import { mapHelper } from "../../helper/mapHelper.ts";
import { $IpcOptions, $OnIpcPool, Ipc, IpcPoolPack, MessagePortIpc, ReadableStreamIpc } from "../index.ts";
import { NativeIpc } from "./NativeIpc.ts";

let ipc_pool_uid_acc = 0;

/**每一个worker 都会创建单独的IpcPool */
export class IpcPool {
  constructor(readonly poolName: string) {
    this.poolId = this.poolName + this.poolId;
    this.initOnMessage();
  }

  readonly poolId = `-worker-${ipc_pool_uid_acc++}`;

  private ipcPool = new Map<string, Ipc>();

  // close start
  protected _closeSignal = createSignal<() => unknown>(false);
  onClose = this._closeSignal.listen;
  // close end
  // deno-lint-ignore no-explicit-any
  private _createSignal<T extends $Callback<any[]>>(autoStart?: boolean) {
    const signal = createSignal<T>(autoStart);
    this.onClose(() => signal.clear());
    return signal;
  }

  /**
   * fork出一个已经创建好通信的ipc
   * @options IpcOptions
   */
  create<T extends Ipc>(
    /**ipc的业务线标识*/
    channelId: string,
    options: $IpcOptions
  ) {
    return mapHelper.getOrPut(this.ipcPool, channelId, () => {
      const mm = options.remote;
      // 创建不同的Ipc
      let ipc: Ipc;
      if (options.port != null) {
        ipc = new MessagePortIpc(options.port, mm, channelId, this);
      } else if (options.channel != null) {
        ipc = new NativeIpc(options.channel, mm, channelId, this);
      } else {
        ipc = new ReadableStreamIpc(mm, channelId, this);
      }
      ipc.start();
      return ipc;
    }) as T;
  }

  // 发送消息   这里是road寻址的前身
  // doPostMessage(channelId: string, data: $IpcMessage) {
  //   // TODO waterbang 这里不想每次获取两个map
  //   const pid = this.ipcChannelMap.get(channelId);
  //   if (pid) {
  //     const ipc = this.ipcHashMap.get(pid);
  //     if (ipc) ipc._doPostMessage(pid, data);
  //   } else {
  //     throw new Error("this channelId $poolId not found!");
  //   }
  // }

  // 收集消息并且转发到各个通道
  private _messageSignal = this._createSignal<$OnIpcPool>(false);

  emitMessage = (message: IpcPoolPack, ipc: Ipc) => this._messageSignal.emit(message, ipc);

  onMessage = this._messageSignal.listen;

  // 分发消息
  initOnMessage() {
    this.onMessage((message, ipc) => {
      ipc.emitMessage(message.ipcMessage);
    });
  }

  /**
   * 根据传进来的业务描述，注册一个Pid
   */
  generatePid(channelId: string): number {
    const time = new Date().getTime();
    const hashPid = hashString(`${channelId}${time}`);
    return hashPid;
  }
}

// 这是一个简单的hashCode实现，用于计算字符串的hashCode
export function hashString(s: string): number {
  let hash = 0;
  for (let i = 0; i < s.length; i++) {
    // 使用charCodeAt获取字符的Unicode值，这个值在0-65535之间
    const charCode = s.charCodeAt(i);
    // 使用了一种称为“旋转哈希”的技术，通过将上一个哈希值左旋然后加上新字符的哈希值来生成新的哈希值
    hash = (hash << 5) - hash + charCode;
    // 使用按位异或运算符将hash值限制在一个32位的整数范围内
    hash = hash & hash;
  }
  return hash;
}

export const workerIpcPool = new IpcPool("desktop");
