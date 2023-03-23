import chalk from "chalk";
import type { $BootstrapContext } from "../../core/bootstrapContext.cjs";
import { ReadableStreamIpc } from "../../core/ipc-web/ReadableStreamIpc.cjs";
import { Ipc, IpcResponse, IPC_ROLE } from "../../core/ipc/index.cjs";
import { MicroModule } from "../../core/micro-module.cjs";
import { httpMethodCanOwnBody } from "../../helper/httpMethodCanOwnBody.cjs";
import type { $IpcSupportProtocols } from "../../helper/types.cjs";
import { buildUrl } from "../../helper/urlHelper.cjs";
import { Native2JsIpc } from "../js-process/ipc.native2js.cjs";

import type { JmmMetadata } from "./JmmMetadata.cjs";

/**
 * 所有的js程序都只有这么一个动态的构造器
 */
export class JsMicroModule extends MicroModule {
  readonly ipc_support_protocols: $IpcSupportProtocols = {
    message_pack: true,
    protobuf: false,
    raw: true,
  };
  constructor(
    /**
     * js程序是动态外挂的
     * 所以需要传入一份配置信息
     */
    readonly metadata: JmmMetadata
  ) {
    super();
  }
  get mmid() {
    return this.metadata.config.id;
  }

  /**
   * 和 dweb 的 port 一样，pid 是我们自己定义的，它跟我们的 mmid 关联在一起
   * 所以不会和其它程序所使用的 pid 冲突
   */
  private _process_id?: string;
  /**
   * 一个 jsMM 可能连接多个模块
   */
  private _remoteIpcs = new Map<string, Ipc>();
  private _workerIpc: Native2JsIpc | undefined;

  /** 每个 JMM 启动都要依赖于某一个js */
  async _bootstrap(context: $BootstrapContext) {
    console.log(`[micro-module.js.ct _bootstrap ${this.mmid}]`);
    // 需要添加 onConenct 这样通过 jsProcess 发送过来的 ipc.posetMessage 能够能够接受的到这个请求
    // 也就是能够接受 匹配的 worker 发送你过来的请求能够接受的到
    this.onConnect((ipc, rease) => {
      console.log(`[micro-module.js.cts ${this.mmid} onConnect]`)
      // ipc === js-process registerCommonIpcOnMessageHandler /create-process" handle 里面的第二个参数ipc
      ipc.onRequest(async (request) => {
        const init = httpMethodCanOwnBody(request.method)
          ? { method: request.method, body: await request.body.stream() }
          : { method: request.method };

        const response = await this.nativeFetch(request.parsed_url.href, init);
        workerIpc.postMessage(
          await IpcResponse.fromResponse(request.req_id, response, workerIpc)
        );
      });

      ipc.onMessage(async (request) => {
        // console.log('ipc.onMessage', request)
      });

      /** 
       * 处理从 js-process.cts 发送过来的 
       * messate.type === IPC_MESSAGE_TYPE.EVENT
       * 
       */
      ipc.onEvent(async (ipcEventMessage, nativeIpc /** nativeIpc === workerIpc */) => {
        console.log(`[micro-module.js.cts ${this.mmid} ipc.onEvent]`, ipcEventMessage)
        if(ipcEventMessage.name === "dns/connect"){
          if(Object.prototype.toString.call(ipcEventMessage.data).slice(8, -1) !== "String") throw new Error('非法的 ipcEvent.data')
          // 创建同 远程模块的 ipc 通道
          const mmid = JSON.parse(ipcEventMessage.data as string).mmid
          const [remoteIpc, localIpc] = await context.dns.connect(mmid)
          this._remoteIpcs.set(mmid, remoteIpc)
          // 如果能够把 remoteIpc 直接返回回去就完美了
          ipc.postMessage(IpcEvent.fromText("dns/connect", "done"))
          // 从连接的模块中收到的 IpcEvent 直接转发
          remoteIpc.onEvent((event, _ipc) => ipc.postMessage(event))
          return;
        }

        // 如何把 发送给
        if(this.mmid === ipcEventMessage.name){
          // console.log(chalk.red(`micro-module.js.cts ipc.onEvent 这里还有问题 还需要处理，无法把消息发送给对应的 worker`), ipcEventMessage, ipc);
          // 测试代码 创建链接
          // 判断是是有已经有了链接
          this._workerIpc = this._workerIpc === undefined ? await  this._beConnect(this) : this._workerIpc;
          this._workerIpc.postMessage(ipcEventMessage)
          // 接受到 从 worker 中返回的消息
          this._workerIpc.onMessage((message, _ipc /** 这个ipc 匹配的是 this._workerIpc*/) => {
            // 把这个消息发送给 ipc
            ipc.postMessage(message)
          })
          
          return;
        }

        const remoteIpc = this._remoteIpcs.get(ipcEventMessage.name)
        if(remoteIpc === undefined) throw new Error(`${this.mmid} 模块 ipc.onEvent 没有匹配的 remoteIpc`)
        remoteIpc.postMessage(ipcEventMessage)
      });
    });

    const pid = Math.ceil(Math.random() * 1000).toString();
    this._process_id = pid;
    const streamIpc = new ReadableStreamIpc(this, IPC_ROLE.SERVER);
    streamIpc.onRequest(async (request) => {
      if (request.parsed_url.pathname.endsWith("/")) {
        streamIpc.postMessage(
          IpcResponse.fromText(
            request.req_id,
            403,
            undefined,
            "Forbidden",
            streamIpc
          )
        );
      } else {
        const main_code = await this.nativeFetch(
          this.metadata.config.server.root + request.parsed_url.pathname
        ).text();

        streamIpc.postMessage(
          IpcResponse.fromText(
            request.req_id,
            200,
            undefined,
            main_code,
            streamIpc
          )
        );
      }
    });

    // console.log("[micro-module.js.cts 执行 bindIncomeStream:]", this.mmid)
    void streamIpc.bindIncomeStream(
      this.nativeFetch(
        buildUrl(new URL(`file://js.sys.dweb/create-process`), {
          search: {
            entry: this.metadata.config.server.entry,
            process_id: pid,
          },
        }),
        {
          method: "POST",
          body: streamIpc.stream,
        }
      ).stream()
    );
    this._connecting_ipcs.add(streamIpc);

    const [jsIpc] = await context.dns.connect("js.sys.dweb");
    jsIpc.onRequest(async (ipcRequest) => {
      const response = await this.nativeFetch(ipcRequest.toRequest());
      jsIpc.postMessage(
        await IpcResponse.fromResponse(ipcRequest.req_id, response, jsIpc)
      );
    });

    jsIpc.onEvent(async (ipcEvent) => {
      console.log("接收到连接的请求");
      if (ipcEvent.name === "dns/connect") {
        const { mmid } = JSON.parse(ipcEvent.text);
        const [targetIpc] = await context.dns.connect(mmid);
        const portId = await this.nativeFetch(
          buildUrl(new URL(`file://js.sys.dweb/create-ipc`), {
            search: { pid, mmid },
          })
        ).number();
        const originIpc = new Native2JsIpc(portId, this);
        /**
         * 将两个消息通道间接互联
         */
        originIpc.onMessage((ipcMessage) => targetIpc.postMessage(ipcMessage));
        targetIpc.onMessage((ipcMessage) => originIpc.postMessage(ipcMessage));
      }
    });
  }
  private _connecting_ipcs = new Set<Ipc>();

  async _beConnect(from: MicroModule): Promise<Native2JsIpc> {
    const process_id = this._process_id;
    if (process_id === undefined) {
      throw new Error("process_id no found.");
    }
    // console.log(chalk.red(`问题从这里开始 process_id === ${this._process_id}`))
    const port_id = await this.nativeFetch(
      `file://js.sys.dweb/create-ipc?process_id=${process_id}&mmid=${this.mmid}`
    ).number();

    const outer_ipc = new Native2JsIpc(port_id, this);
    this._connecting_ipcs.add(outer_ipc);
    this._workerIpc = outer_ipc; /** 测试代码 */
    return outer_ipc;
  }

  _shutdown() {
    for (const outer_ipc of this._connecting_ipcs) {
      outer_ipc.close();
    }
    this._connecting_ipcs.clear();

    /**
     * @TODO 发送指令，关停js进程
     */
    this._process_id = undefined;
  }
}
