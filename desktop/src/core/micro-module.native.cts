import {
  $deserializeRequestToParams,
  $serializeResultToResponse,
} from "./helper.cjs";
import {
  Ipc,
  IpcRequest,
  IpcResponse,
  IPC_DATA_TYPE,
  IPC_ROLE,
} from "./ipc.cjs";
import { NativeIpc } from "./ipc.native.cjs";
import { MicroModule } from "./micro-module.cjs";
import type {
  $PromiseMaybe,
  $Schema1,
  $Schema1ToType,
  $Schema2,
  $Schema2ToType,
} from "./types.cjs";

export abstract class NativeMicroModule extends MicroModule {
  abstract override mmid: `${string}.${"sys" | "std"}.dweb`;
  private _connectting_ipcs = new Set<Ipc>();
  _connect(): NativeIpc {
    const channel = new MessageChannel();
    const { port1, port2 } = channel;
    const inner_ipc = new NativeIpc(port2, this, IPC_ROLE.SERVER);

    this._connectting_ipcs.add(inner_ipc);
    inner_ipc.onClose(() => {
      this._connectting_ipcs.delete(inner_ipc);
    });

    this._emitConnect(inner_ipc);
    return new NativeIpc(port1, this, IPC_ROLE.CLIENT);
  }

  /**
   * 内部程序与外部程序通讯的方法
   * TODO 这里应该是可以是多个
   */
  protected _on_connect_cbs = new Set<(ipc: Ipc) => unknown>();
  /**
   * 给内部程序自己使用的 onConnect，外部与内部建立连接时使用
   * 因为 NativeMicroModule 的内部程序在这里编写代码，所以这里会提供 onConnect 方法
   * 如果时 JsMicroModule 这个 onConnect 就是写在 WebWorker 那边了
   */
  protected onConnect(cb: (ipc: Ipc) => unknown) {
    this._on_connect_cbs.add(cb);
    return () => this._on_connect_cbs.delete(cb);
  }
  protected _emitConnect(ipc: Ipc) {
    for (const cb of this._on_connect_cbs) {
      cb(ipc);
    }
  }

  override after_shutdown() {
    super.after_shutdown();
    for (const inner_ipc of this._connectting_ipcs) {
      inner_ipc.close();
    }
    this._connectting_ipcs.clear();
  }

  ///

  private _commmon_ipc_on_message_hanlders =
    new Set<$RequestCustomHanlderSchema>();
  private _inited_commmon_ipc_on_message = false;
  private _initCommmonIpcOnMessage() {
    if (this._inited_commmon_ipc_on_message) {
      return;
    }
    this._inited_commmon_ipc_on_message = true;

    this.onConnect((client_ipc) => {
      client_ipc.onMessage(async (request) => {
        if (request.type !== IPC_DATA_TYPE.REQUEST) {
          return;
        }
        const { pathname } = request.parsed_url;
        let response: IpcResponse | undefined;
        for (const hanlder_schema of this._commmon_ipc_on_message_hanlders) {
          if (
            hanlder_schema.matchMode === "full"
              ? pathname === hanlder_schema.pathname
              : hanlder_schema.matchMode === "prefix"
              ? pathname.startsWith(hanlder_schema.pathname)
              : false
          ) {
            try {
              const result = await hanlder_schema.hanlder(
                hanlder_schema.input(request),
                client_ipc
              );
              if (result instanceof IpcResponse) {
                response = result;
              } else {
                response = hanlder_schema.output(request, result);
              }
            } catch (err) {
              let body: string;
              if (err instanceof Error) {
                body = err.message;
              } else {
                body = String(err);
              }
              response = IpcResponse.fromJson(request.req_id, 500, body);
            }
            break;
          }
        }
        if (response === undefined) {
          response = IpcResponse.fromText(
            request.req_id,
            404,
            `no found hanlder for '${pathname}'`
          );
        }
        client_ipc.postMessage(response);
      });
    });
  }
  protected registerCommonIpcOnMessageHanlder<
    I extends $Schema1,
    O extends $Schema2
  >(common_hanlder_schema: $RequestCommonHanlderSchema<I, O>) {
    this._initCommmonIpcOnMessage();
    const hanlders = this._commmon_ipc_on_message_hanlders;
    const custom_handler_schema: $RequestCustomHanlderSchema<any, any> = {
      ...common_hanlder_schema,
      input: $deserializeRequestToParams(common_hanlder_schema.input),
      output: $serializeResultToResponse(common_hanlder_schema.output),
    };
    /// 初始化
    hanlders.add(custom_handler_schema);
    return () => hanlders.delete(custom_handler_schema);
  }
}

export type $RequestCommonHanlderSchema<
  I extends $Schema1,
  O extends $Schema2
> = {
  readonly pathname: string;
  readonly matchMode: "full" | "prefix";
  readonly input: I;
  readonly output: O;
  readonly hanlder: (
    args: $Schema1ToType<I>,
    client_ipc: Ipc
  ) => $PromiseMaybe<$Schema2ToType<O> | IpcResponse>;
};

export type $RequestCustomHanlderSchema<ARGS = unknown, RES = unknown> = {
  readonly pathname: string;
  readonly matchMode: "full" | "prefix";
  readonly input: (request: IpcRequest) => ARGS;
  readonly output: (request: IpcRequest, result: RES) => IpcResponse;
  readonly hanlder: (
    args: ARGS,
    client_ipc: Ipc
  ) => $PromiseMaybe<RES | IpcResponse>;
};
