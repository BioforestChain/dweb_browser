import { $deserializeRequestToParams } from "../helper/$deserializeRequestToParams.cjs";
import { $isMatchReq, $ReqMatcher } from "../helper/$ReqMatcher.cjs";
import { $serializeResultToResponse } from "../helper/$serializeResultToResponse.cjs";
import { createSignal } from "../helper/createSignal.cjs";
import type {
  $PromiseMaybe,
  $Schema1,
  $Schema1ToType,
  $Schema2,
  $Schema2ToType,
} from "../helper/types.cjs";
import { NativeIpc } from "./ipc.native.cjs";
import { Ipc, IpcRequest, IpcResponse, IPC_ROLE } from "./ipc/index.cjs";
import { MicroModule } from "./micro-module.cjs";
import chalk from "chalk";

export abstract class NativeMicroModule extends MicroModule {
  abstract override mmid: `${string}.${"sys" | "std"}.dweb`;
  private _connectting_ipcs = new Set<Ipc>();
  _connect(from: MicroModule): NativeIpc {
    const channel = new MessageChannel();
    const { port1, port2 } = channel;
    const inner_ipc = new NativeIpc(port2, from, IPC_ROLE.SERVER);

    this._connectting_ipcs.add(inner_ipc);
    inner_ipc.onClose(() => {
      this._connectting_ipcs.delete(inner_ipc);
    });

    this._connectSignal.emit(inner_ipc);
    return new NativeIpc(port1, this, IPC_ROLE.CLIENT);
  }

  /**
   * 内部程序与外部程序通讯的方法
   * TODO 这里应该是可以是多个
   */
  protected _connectSignal = createSignal<(ipc: Ipc) => unknown>();
  /**
   * 给内部程序自己使用的 onConnect，外部与内部建立连接时使用
   * 因为 NativeMicroModule 的内部程序在这里编写代码，所以这里会提供 onConnect 方法
   * 如果时 JsMicroModule 这个 onConnect 就是写在 WebWorker 那边了
   */
  protected onConnect = this._connectSignal.listen;

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
      client_ipc.onRequest(async (request) => {
        const { pathname } = request.parsed_url;
        let response: IpcResponse | undefined;
        // 添加了一个判断 如果没有注册匹配请求的监听器会有信息弹出到 终端;
        let has = false;
        for (const hanlder_schema of this._commmon_ipc_on_message_hanlders) {
          if ($isMatchReq(hanlder_schema, pathname, request.method)) {
            has = true;
            try {
            
              const result = await hanlder_schema.handler(
                hanlder_schema.input(request),
                client_ipc,
                request
              );
              
              if (result instanceof IpcResponse) {
                response = result;
              } else {
                response = await hanlder_schema.output(
                  request,
                  result,
                  client_ipc
                );
              }
            } catch (err) {
              console.log('err: ', err)
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

        if(!has) { /** 没有匹配的事件处理器 弹出终端 优化了开发体验 */
          console.log(chalk.red('[micro-module.native.cts 没有匹配的注册方法 mmid===]',this.mmid), "请求的方法是",request);
        }

        if (response === undefined) {
          response = IpcResponse.fromText(
            request.req_id,
            404,
            `no found handler for '${pathname}'`
          );
        }
        client_ipc.postMessage(response);
      });
    });
  }
  protected registerCommonIpcOnMessageHandler<
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

interface $RequestHanlderSchema<ARGS, RES> extends $ReqMatcher {
  readonly handler: (
    args: ARGS,
    client_ipc: Ipc,
    ipc_request: IpcRequest
  ) => $PromiseMaybe<RES | IpcResponse>;
}

export interface $RequestCommonHanlderSchema<
  I extends $Schema1,
  O extends $Schema2
> extends $RequestHanlderSchema<$Schema1ToType<I>, $Schema2ToType<O>> {
  readonly input: I;
  readonly output: O;
}

export interface $RequestCustomHanlderSchema<ARGS = unknown, RES = unknown>
  extends $RequestHanlderSchema<ARGS, RES> {
  readonly input: (request: IpcRequest) => ARGS;
  readonly output: (
    request: IpcRequest,
    result: RES,
    ipc: Ipc
  ) => $PromiseMaybe<IpcResponse>;
}
