import { ReadableStreamIpc } from "../core/ipc-web/ReadableStreamIpc.cjs";
import { Ipc, IpcResponse, IPC_ROLE } from "../core/ipc/index.cjs";
import { MicroModule } from "../core/micro-module.cjs";
import type { $MMID } from "../helper/types.cjs";
import { buildUrl } from "../helper/urlHelper.cjs";
import { Native2JsIpc } from "./js-process/ipc.native2js.cjs";

/**
 * 所有的js程序都只有这么一个动态的构造器
 */
export class JsMicroModule extends MicroModule {
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

  private _process_id?: number;

  /** 每个 JMM 启动都要依赖于某一个js */
  async _bootstrap() {
    const streamIpc = new ReadableStreamIpc(this, IPC_ROLE.SERVER);
    streamIpc.onRequest(async (request) => {
      if (request.parsed_url.pathname === "/index.js") {
        const main_code = await this.fetch(this.metadata.main_url).text();

        streamIpc.postMessage(
          IpcResponse.fromText(request.req_id, 200, main_code)
        );
      } else {
        streamIpc.postMessage(IpcResponse.fromText(request.req_id, 404, ""));
      }
    });
    void streamIpc.bindIncomeStream(
      this.fetch(
        buildUrl(new URL(`file://js.sys.dweb/create-process`), {
          search: { main_pathname: "/index.js" },
        }),
        {
          method: "POST",
          body: streamIpc.stream,
        }
      ).stream()
    );
  }
  private _connectting_ipcs = new Set<Ipc>();
  async _connect(from: MicroModule): Promise<Native2JsIpc> {
    const process_id = this._process_id;
    if (process_id === undefined) {
      throw new Error("process_id no found.");
    }
    const port_id = await this.fetch(
      `file://js.sys.dweb/create-ipc?process_id=${process_id}`
    ).number();
    return new Native2JsIpc(port_id, this);
  }

  _shutdown() {
    for (const inner_ipc of this._connectting_ipcs) {
      inner_ipc.close();
    }
    this._connectting_ipcs.clear();
  }
}
