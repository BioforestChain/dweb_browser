import type { Ipc } from "../core/ipc/index.cjs";
import { MicroModule } from "../core/micro-module.cjs";
import type { $MMID } from "../helper/types.cjs";
import { JsIpc } from "./js-process.cjs";

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
    const process_id = (this._process_id = await this.fetch(
      `file://js.sys.dweb/create-process?main_code=${encodeURIComponent(
        await fetch(this.metadata.main_url).then((res) => res.text())
      )}`
    ).number());
  }
  private _connectting_ipcs = new Set<Ipc>();
  async _connect(from: MicroModule): Promise<JsIpc> {
    const process_id = this._process_id;
    if (process_id === undefined) {
      throw new Error("process_id no found.");
    }
    const port_id = await this.fetch(
      `file://js.sys.dweb/create-ipc?process_id=${process_id}`
    ).number();
    return new JsIpc(port_id, this);
  }

  _shutdown() {
    for (const inner_ipc of this._connectting_ipcs) {
      inner_ipc.close();
    }
    this._connectting_ipcs.clear();
  }
}
