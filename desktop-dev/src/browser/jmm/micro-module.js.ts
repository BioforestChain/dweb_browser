import type { $BootstrapContext } from "../../core/bootstrapContext.ts";
import { ReadableStreamIpc } from "../../core/ipc-web/ReadableStreamIpc.ts";
import { IPC_ROLE, Ipc, IpcResponse } from "../../core/ipc/index.ts";
import { MicroModule } from "../../core/micro-module.ts";
import { connectAdapterManager } from "../../core/nativeConnect.ts";
import type { $DWEB_DEEPLINK, $IpcSupportProtocols, $MMID } from "../../core/types.ts";
import { PromiseOut } from "../../helper/PromiseOut.ts";
import { mapHelper } from "../../helper/mapHelper.ts";
import { buildUrl } from "../../helper/urlHelper.ts";
import { Native2JsIpc } from "../js-process/ipc.native2js.ts";

type $JsMM = { jmm: JsMicroModule; remoteMm: MicroModule };

const nativeToWhiteList = new Set<$MMID>(["js.browser.dweb"]);
connectAdapterManager.append(async (fromMM, toMM, reason) => {
  let jsMM: null | $JsMM = null;
  if (nativeToWhiteList.has(toMM.mmid)) {
    /// 白名单，忽略
  } else if (toMM instanceof JsMicroModule) {
    jsMM = { jmm: toMM, remoteMm: fromMM };
  } else if (fromMM instanceof JsMicroModule) {
    jsMM = { jmm: fromMM, remoteMm: toMM };
  }
  if (jsMM != null) {
    /**
     * 与 NMM 相比，这里会比较难理解：
     * 因为这里是直接创建一个 Native2JsIpc 作为 ipcForFromMM，
     * 而实际上的 ipcForToMM ，是在 js-context 里头去创建的，因此在这里是 一个假的存在
     *
     * 也就是说。如果是 jsMM 内部自己去执行一个 connect，那么这里返回的 ipcForFromMM，其实还是通往 js-context 的， 而不是通往 toMM的。
     * 也就是说，能跟 toMM 通讯的只有 js-context，这里无法通讯。
     */
    const originIpc = await jsMM.jmm.ipcBridge(jsMM.remoteMm.mmid).promise;
    fromMM.beConnect(originIpc, reason);
    toMM.beConnect(originIpc, reason);

    return [originIpc, originIpc];
  }
}, 1);
/**
 * 所有的js程序都只有这么一个动态的构造器
 */
export class JsMicroModule extends MicroModule {
  readonly ipc_support_protocols: $IpcSupportProtocols = {
    cbor: true,
    protobuf: false,
    raw: true,
  };
  constructor(
    /**
     * js程序是动态外挂的
     * 所以需要传入一份配置信息
     */
    readonly metadata: JsMMMetadata
  ) {
    super();
  }
  get mmid() {
    return this.metadata.config.id;
  }
  get dweb_deeplinks() {
    return this.metadata.config.dweb_deeplinks ?? [];
  }

  /**
   * 和 dweb 的 port 一样，pid 是我们自己定义的，它跟我们的 mmid 关联在一起
   * 所以不会和其它程序所使用的 pid 冲突
   */
  private _process_id?: string;

  /** 每个 JMM 启动都要依赖于某一个js */
  async _bootstrap(context: $BootstrapContext) {
    console.log("jsmm", `[${this.metadata.config.id} micro-module.js.ct _bootstrap ${this.mmid}]`);
    const pid = Math.ceil(Math.random() * 1000).toString();
    this._process_id = pid;
    // 这个 streamIpc 专门服务于 file://js.browser.dweb/create-process
    const streamIpc = new ReadableStreamIpc(this, IPC_ROLE.SERVER);
    // 用来提供 JsMicroModule 匹配的 worker.js 代码
    streamIpc.onRequest(async (request) => {
      if (request.parsed_url.pathname.endsWith("/")) {
        streamIpc.postMessage(IpcResponse.fromText(request.req_id, 403, undefined, "Forbidden", streamIpc));
      } else {
        // 获取 worker.js 代码
        const main_code = await this.nativeFetch(this.metadata.config.server.root + request.parsed_url.pathname).text();

        streamIpc.postMessage(IpcResponse.fromText(request.req_id, 200, undefined, main_code, streamIpc));
      }
    });

    // 创建一个 streamIpc
    void streamIpc.bindIncomeStream(
      this.nativeFetch(
        buildUrl(new URL(`file://js.browser.dweb/create-process`), {
          search: {
            entry: this.metadata.config.server.entry,
            process_id: this._process_id,
          },
        }),
        {
          method: "POST",
          body: streamIpc.stream,
        }
      ).stream()
    );
    this.addToIpcSet(streamIpc);

    const [jsIpc] = await context.dns.connect("js.browser.dweb");

    jsIpc.onClose(() => {
      this.shutdown();
    });

    jsIpc.onRequest(async (ipcRequest, ipc) => {
      /// WARN 这里不再受理 file://<domain>/ 的请求，只处理 http[s]:// | file:/// 这些原生的请求
      const protocol = ipcRequest.parsed_url.protocol;
      const host = ipcRequest.parsed_url.host;
      if (protocol === "file:" && host.endsWith(".dweb")) {
        const connectResult = this.connect(host as $MMID);
        if (!connectResult) throw new Error(`not found NMM ${host}`);
        const [jsWebIpc] = await connectResult;
        jsWebIpc.emitMessage(ipcRequest);
      } else {
        const request = ipcRequest.toRequest();
        const response = await this.nativeFetch(request);
        const newResponse = await IpcResponse.fromResponse(ipcRequest.req_id, response, jsIpc);
        jsIpc.postMessage(newResponse);
      }
    });

    jsIpc.onEvent(async (ipcEvent) => {
      if (ipcEvent.name === "dns/connect") {
        const { mmid } = JSON.parse(ipcEvent.text);
        try {
          /**
           * 模块之间的ipc是单例模式，所以我们必须拿到这个单例，再去做消息转发
           * 但可以优化的点在于：TODO 我们应该将两个连接的协议进行交集，得到最小通讯协议，然后两个通道就能直接通讯raw数据，而不需要在转发的时候再进行一次编码解码
           *
           * 此外这里允许js多次建立ipc连接，因为可能存在多个js线程，它们是共享这个单例ipc的
           */
          /**
           * 向目标模块发起连接，注意，这里是很特殊的，因为我们自定义了 JMM 的连接适配器 connectAdapterManager，
           * 所以 JsMicroModule 这里作为一个中间模块，是没法直接跟其它模块通讯的。
           *
           * TODO 如果有必要，未来需要让 connect 函数支持 force 操作，支持多次连接。
           */
          const [targetIpc] = await context.dns.connect(mmid);
          /// 只要不是我们自己创建的直接连接的通道，就需要我们去创造直连并进行桥接
          if (targetIpc.remote.mmid != this.mmid) {
            await this.ipcBridge(mmid, targetIpc).promise;
          }
        } catch (err) {
          this.ipcConnectFail(mmid, err);
        }
      }
      if (ipcEvent.name === "restart") {
        // 调用重启
        context.dns.restart(this.mmid);
      }
    });
  }
  private _fromMmid_originIpc_map = new Map<$MMID, PromiseOut<Ipc>>();
  ipcBridge(fromMmid: $MMID, targetIpc?: Ipc) {
    return mapHelper.getOrPut(this._fromMmid_originIpc_map, fromMmid, () => {
      const task = new PromiseOut<Ipc>();
      (async () => {
        try {
          /**
           * 向js模块发起连接
           */
          const portId = await this.nativeFetch(
            buildUrl(new URL(`file://js.browser.dweb/create-ipc`), {
              search: { process_id: this._process_id, mmid: fromMmid },
            })
          ).number();

          const originIpc = new JmmIpc(portId, this);

          /// 如果传入了 targetIpc，那么启动桥接模式，我们会中转所有的消息给 targetIpc，
          /// 包括关闭，那么这个 targetIpc 理论上就可以作为 originIpc 的代理
          if (targetIpc !== undefined) {
            /**
             * 将两个消息通道间接互联
             */
            originIpc.onMessage((ipcMessage) => {
              targetIpc.postMessage(ipcMessage);
            });
            targetIpc.onMessage((ipcMessage) => {
              originIpc.postMessage(ipcMessage);
            });

            /**
             * 监听关闭事件
             */
            originIpc.onClose(() => {
              this._fromMmid_originIpc_map.delete(originIpc.remote.mmid);
              targetIpc.close();
            });
            targetIpc.onClose(() => {
              this._fromMmid_originIpc_map.delete(targetIpc.remote.mmid);
              originIpc.close();
            });
          } else {
            originIpc.onClose(() => {
              this._fromMmid_originIpc_map.delete(fromMmid);
            });
          }
          task.resolve(originIpc);
        } catch (e) {
          console.error("_ipcBridge", e);
          task.reject(e);
        }
      })().catch(task.reject);
      return task;
    });
  }

  async ipcConnectFail(mmid: $MMID, reason: unknown) {
    let errMessage = "";
    if (reason instanceof Error) {
      errMessage = reason.message + "\n" + (reason.stack || "");
    } else {
      errMessage = String(reason);
    }
    /**
     * 向js模块发起连接
     */
    return await this.nativeFetch(
      buildUrl(new URL(`file://js.browser.dweb/create-ipc-fail`), {
        search: {
          process_id: this._process_id,
          mmid: mmid,
          reason: errMessage,
        },
      })
    ).boolean();
  }


  _shutdown() {
      /// 发送指令，关停js进程
      this.nativeFetch("file://js.browser.dweb/close-all-process")
    // 删除 _fromMmid_originIpc_map 里面的ipc
    Array.from(this._fromMmid_originIpc_map.values()).forEach(async (item) => {
      (await item.promise).close();
    });

    this._process_id = undefined;
  }
}
class JmmIpc extends Native2JsIpc {}

export class JsMMMetadata {
  constructor(readonly config: $JsMMMetadata) {}
}
export interface $JsMMMetadata {
  id: $MMID;
  server: $JsMMMetadata.$MainServer;
  dweb_deeplinks?: $DWEB_DEEPLINK[];
}

declare namespace $JsMMMetadata {
  export interface $MainServer {
    root: string;
    entry: string;
  }
}
