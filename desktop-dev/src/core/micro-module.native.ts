import chalk from "chalk";
import { $deserializeRequestToParams } from "../helper/$deserializeRequestToParams.ts";
import { $isMatchReq, $ReqMatcher } from "../helper/$ReqMatcher.ts";
import { $serializeResultToResponse } from "../helper/$serializeResultToResponse.ts";
import type {
  $DWEB_DEEPLINK,
  $IpcSupportProtocols,
  $PromiseMaybe,
  $Schema1,
  $Schema1ToType,
  $Schema2,
  $Schema2ToType,
} from "../helper/types.ts";
import { NativeIpc } from "./ipc.native.ts";
import { Ipc, IpcRequest, IpcResponse, IPC_ROLE } from "./ipc/index.ts";
import { MicroModule } from "./micro-module.ts";
import { connectAdapterManager } from "./nativeConnect.ts";

connectAdapterManager.append((fromMM, toMM, reason) => {
  // 测试代码
  if (toMM instanceof NativeMicroModule) {
    const channel = new MessageChannel();
    const { port1, port2 } = channel;
    const toNativeIpc = new NativeIpc(port1, fromMM, IPC_ROLE.SERVER);
    const fromNativeIpc = new NativeIpc(port2, toMM, IPC_ROLE.CLIENT);
    fromMM.beConnect(fromNativeIpc, reason); // 通知发起连接者作为Client
    toMM.beConnect(toNativeIpc, reason); // 通知接收者作为Server
    return [fromNativeIpc, toNativeIpc];
  }
});

export abstract class NativeMicroModule extends MicroModule {
  readonly ipc_support_protocols: $IpcSupportProtocols = {
    message_pack: true,
    protobuf: true,
    raw: true,
  };
  readonly dweb_deeplinks: $DWEB_DEEPLINK[] = [];
  abstract override mmid: `${string}.${"sys" | "std"}.dweb`;
  _onConnect(ipc: Ipc) {}

  private _commmon_ipc_on_message_hanlders =
    new Set<$RequestCustomHanlderSchema>();
  private _inited_commmon_ipc_on_message = false;
  private _initCommmonIpcOnMessage() {
    if (this._inited_commmon_ipc_on_message) {
      return;
    }
    this._inited_commmon_ipc_on_message = true;

    this.onConnect((client_ipc) => {
      this._onConnect(client_ipc);
      client_ipc.onRequest(async (request) => {
        const { pathname, protocol } = request.parsed_url;
        let response: IpcResponse | undefined;
        // 添加了一个判断 如果没有注册匹配请求的监听器会有信息弹出到 终端;
        let has = false;
        for (const hanlder_schema of this._commmon_ipc_on_message_hanlders) {
          if ($isMatchReq(hanlder_schema, pathname, request.method, protocol)) {
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
              let body: string;
              if (err instanceof Error) {
                body = err.stack ?? err.message;
              } else {
                body = String(err);
              }
              response = IpcResponse.fromJson(
                request.req_id,
                500,
                undefined,
                body,
                client_ipc
              );
            }
            break;
          }
        }

        if (!has) {
          /** 没有匹配的事件处理器 弹出终端 优化了开发体验 */
          console.log(
            chalk.red(
              "[micro-module.native.cts 没有匹配的注册方法 mmid===]",
              this.mmid
            ),
            "请求的方法是",
            request
          );
        }

        if (response === undefined) {
          response = IpcResponse.fromText(
            request.req_id,
            404,
            undefined,
            `no found hanlder for '${pathname}'`,
            client_ipc
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
