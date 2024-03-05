import { $Callback, createSignal } from "../../helper/createSignal.ts";
import { $IpcMessage, $OnIpcPool, Ipc, IpcPoolPack } from "../index.ts";

let ipc_pool_uid_acc = 0;

/**每一个worker 都会创建单独的IpcPool */
export class IpcPool {
  constructor() {
    this.distribute();
  }

  readonly poolId = `worker-${ipc_pool_uid_acc++}`;

  // close start
  protected _closeSignal = createSignal<() => unknown>(false);
  onClose = this._closeSignal.listen;
  // close end
  private _createSignal<T extends $Callback<any[]>>(autoStart?: boolean) {
    const signal = createSignal<T>(autoStart);
    this.onClose(() => signal.clear());
    return signal;
  }
  private ipcChannelMap = new Map<string, number>();
  private ipcHashMap = new Map<number, Ipc>();

  /**
   * fork出一个已经创建好通信的ipc
   * @options IpcOptions
   */
  // create(
  //   /**ipc的业务线标识*/
  //   channelId: string,
  //   options: $IpcOptions
  // ) {
  //   const mm = options.remote;
  //   // 创建不同的Ipc
  //   let ipc: Ipc;
  //   if (options.port != null) {
  //     ipc = new MessagePortIpc(options.port, mm, channelId, this);
  //   } else if (options.channel != null) {
  //     ipc = new NativeIpc(options.channel, mm, channelId, this);
  //   } else {
  //     ipc = new ReadableStreamIpc(mm, channelId, this);
  //   }
  //   if (ipc instanceof ReadableStreamIpc && options.stream != null) {
  //     ipc.bindIncomeStream(options.stream);
  //   }
  //   //  有新的ipc激活了
  //   const pid = this.generatePid(channelId);
  //   this.ipcHashMap.set(pid, ipc);
  //   ipc.lifeCycleHook();
  //   // 如果还没启动，自我启动一下
  //   if (!ipc.startDeferred.is_resolved) {
  //     if (!(ipc instanceof ReadableStreamIpc && !ipc.isBinding)) {
  //       ipc.start();
  //     }
  //   }
  //   ipc;
  // }

  // 发送消息   这里是road寻址的前身
  doPostMessage(channelId: String, data: $IpcMessage) {}

  // 收集消息并且转发到各个通道
  private _messageSignal = this._createSignal<$OnIpcPool>(false);

  emitMessage = (message: IpcPoolPack, ipc: Ipc) => this._messageSignal.emit(message, ipc);

  onMessage = this._messageSignal.listen;

  // 分发消息
  distribute() {
    this.onMessage((message, ipc) => {});
  }

  /**
   * 根据传进来的业务描述，注册一个Pid
   */
  generatePid(channelId: string): number {
    const pid = this.ipcChannelMap.get(channelId);
    if (pid != null) {
      return pid;
    }
    const hashPid = hashString(`${channelId}${pid}`);
    this.ipcChannelMap.set(channelId, hashPid);
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

export const workerIpcPool = new IpcPool();
