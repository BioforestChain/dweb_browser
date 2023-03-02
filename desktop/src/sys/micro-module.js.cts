import { ReadableStreamIpc } from "../core/ipc-web/ReadableStreamIpc.cjs";
import { Ipc, IpcResponse, IPC_ROLE } from "../core/ipc/index.cjs";
import { MicroModule } from "../core/micro-module.cjs";
import type { $IpcSupportProtocols, $MMID } from "../helper/types.cjs";
import { buildUrl } from "../helper/urlHelper.cjs";
import { Native2JsIpc } from "./js-process/ipc.native2js.cjs";

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
    readonly mmid: $MMID,
    /**
     * js程序是动态外挂的
     * 所以需要传入一份配置信息
     */
    readonly metadata: Readonly<{ main_url: string }>
  ) {
    super();
  }

  /**
   * 和 dweb 的 port 一样，pid 是我们自己定义的，它跟我们的 mmid 关联在一起
   * 所以不会和其它程序所使用的 pid 冲突
   */
  private _process_id?: number;

  /** 每个 JMM 启动都要依赖于某一个js */
  async _bootstrap() {
    // console.log("[micro-module.js.cts _bootstrap:]", this.mmid)
    const streamIpc = new ReadableStreamIpc(this, IPC_ROLE.SERVER);
    // console.log("[micro-module.js.cts 执行 onRequest:]", this.mmid)
    streamIpc.onRequest(async (request) => {
      
      // console.log("[micro-module.js.cts 监听到了请求:]", this.mmid)
      if (request.parsed_url.pathname === "/index.js") {
        const main_code = await this.fetch(this.metadata.main_url).text();

        streamIpc.postMessage(
          IpcResponse.fromText(
            request.req_id,
            200,
            undefined,
            main_code,
            streamIpc
          )
        );
      } else {
        streamIpc.postMessage(
          IpcResponse.fromText(request.req_id, 404, undefined, "", streamIpc)
        );
      }
    });
    // console.log("[micro-module.js.cts 执行 bindIncomeStream:]", this.mmid)
    void streamIpc.bindIncomeStream(
      this.fetch(
        buildUrl(new URL(`file://js.sys.dweb/create-process`), {
          search: {
            main_pathname: "/index.js",
            process_id: (this._process_id = Math.ceil(Math.random() * 1000)),
          },
        }),
        {
          method: "POST",
          body: streamIpc.stream,
        }
      ).stream()
    );
    this._connecting_ipcs.add(streamIpc);
  }
  private _connecting_ipcs = new Set<Ipc>();
  async _connect(from: MicroModule): Promise<Native2JsIpc> {
    const process_id = this._process_id;
    if (process_id === undefined) {
      throw new Error("process_id no found.");
    }
    const port_id = await this.fetch(
      `file://js.sys.dweb/create-ipc?process_id=${process_id}`
    ).number();
    const outer_ipc = new Native2JsIpc(port_id, this);
    this._connecting_ipcs.add(outer_ipc);
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
