import { $OffListener } from "../helper/createSignal.ts";
import { $deserializeRequestToParams } from "./helper/$deserializeRequestToParams.ts";
import { $isMatchReq, $ReqMatcher } from "./helper/$ReqMatcher.ts";
import { $serializeResultToResponse } from "./helper/$serializeResultToResponse.ts";
import type { MICRO_MODULE_CATEGORY } from "./helper/category.const.ts";
import { $OnFetch, createFetchHandler } from "./helper/ipcFetchHelper.ts";
import type { $PromiseMaybe, $Schema1, $Schema1ToType, $Schema2, $Schema2ToType } from "./helper/types.ts";
import { workerIpcPool } from "./index.ts";
import { Ipc, IpcRequest, IpcResponse } from "./ipc/index.ts";
import { MicroModule } from "./micro-module.ts";
import { connectAdapterManager } from "./nativeConnect.ts";
import type { $DWEB_DEEPLINK, $IpcSupportProtocols, $MMID } from "./types.ts";

connectAdapterManager.append((fromMM, toMM, reason) => {
  if (toMM instanceof NativeMicroModule) {
    const channel = new MessageChannel();
    const { port1, port2 } = channel;
    const toNativeIpc = workerIpcPool.create(`native-connect-from-${fromMM.mmid}`, {
      remote: fromMM,
      port: port1,
    });
    const fromNativeIpc = workerIpcPool.create(`native-connect-to-${toMM.mmid}`, {
      remote: toMM,
      port: port2,
    });
    fromMM.beConnect(fromNativeIpc, reason); // 通知发起连接者作为Client
    toMM.beConnect(toNativeIpc, reason); // 通知接收者作为Server
    return [fromNativeIpc, toNativeIpc];
  }
});

export abstract class NativeMicroModule extends MicroModule {
  readonly ipc_support_protocols: $IpcSupportProtocols = {
    cbor: true,
    protobuf: true,
    raw: true,
  };
  readonly dweb_deeplinks: $DWEB_DEEPLINK[] = [];
  readonly categories: MICRO_MODULE_CATEGORY[] = [];
  abstract override mmid: $MMID;
  abstract override name: MicroModule["name"];
  override dir: MicroModule["dir"];
  override lang: MicroModule["lang"];
  override short_name: MicroModule["short_name"];
  override description: MicroModule["description"];
  override icons: MicroModule["icons"];
  override screenshots: MicroModule["screenshots"];
  override display: MicroModule["display"] = "standalone";
  override orientation: MicroModule["orientation"];
  override theme_color: MicroModule["theme_color"];
  override background_color: MicroModule["background_color"];
  override shortcuts: MicroModule["shortcuts"];

  private _commmon_ipc_on_message_handlers = new Set<$RequestCustomHanlderSchema>();
  private _inited_commmon_ipc_on_message = false;
  private _initCommmonIpcOnMessage() {
    if (this._inited_commmon_ipc_on_message) {
      return;
    }
    this._inited_commmon_ipc_on_message = true;

    this.onConnect((client_ipc) => {
      client_ipc.onRequest(async (request) => {
        const { pathname, protocol } = request.parsed_url;
        let response: IpcResponse | undefined;
        // 添加了一个判断 如果没有注册匹配请求的监听器会有信息弹出到 终端;
        let has = false;
        for (const handler_schema of this._commmon_ipc_on_message_handlers) {
          if ($isMatchReq(handler_schema, pathname, request.method, protocol)) {
            has = true;
            try {
              const result = await handler_schema.handler(handler_schema.input(request), client_ipc, request);

              if (result instanceof IpcResponse) {
                response = result;
              } else if (result !== null && result !== undefined) {
                response = await handler_schema.output(request, result, client_ipc);
              }
            } catch (err) {
              console.error("error", "IPC-REQ-ERR:", err);
              let body: string;
              if (err instanceof Error) {
                body = err.stack ?? err.message;
              } else {
                body = String(err);
              }
              response = IpcResponse.fromJson(request.reqId, 500, undefined, body, client_ipc);
            }
            break;
          }
        }

        if (response === undefined) {
          // response = IpcResponse.fromText(
          //   request.reqId,
          //   404,
          //   undefined,
          //   `no found handler for '${pathname}'`,
          //   client_ipc
          // );
          return;
        }
        client_ipc.postMessage(response);
      });
    });
  }
  protected registerCommonIpcOnMessageHandler<I extends $Schema1, O extends $Schema2>(
    common_handler_schema: $RequestCommonHanlderSchema<I, O>
  ) {
    this._initCommmonIpcOnMessage();
    const handlers = this._commmon_ipc_on_message_handlers;
    // deno-lint-ignore no-explicit-any
    const custom_handler_schema: $RequestCustomHanlderSchema<any, any> = {
      ...common_handler_schema,
      input: $deserializeRequestToParams(common_handler_schema.input),
      output: $serializeResultToResponse(common_handler_schema.output),
    };
    /// 初始化
    handlers.add(custom_handler_schema);
    return () => handlers.delete(custom_handler_schema);
  }

  /**
   * 监听 IpcRequest 请求，封装成 fetchEvent，通过返回 ResponseInit、Response 对象即可进行响应
   * 再模块销毁后，监听也会被取消
   * @param handlers
   * @returns
   */
  protected onFetch(...handlers: $OnFetch[]) {
    const onRequestHandler = createFetchHandler(handlers);
    const offs: $OffListener[] = [];
    offs.push(
      this.onConnect((client_ipc) => {
        offs.push(client_ipc.onRequest(onRequestHandler));
      })
    );
    const off = () => {
      for (const off of offs) {
        off();
      }
    };
    this.onAfterShutdown(off);
    return Object.assign(off, onRequestHandler);
  }
}

interface $RequestHanlderSchema<ARGS, RES> extends $ReqMatcher {
  readonly handler: (args: ARGS, client_ipc: Ipc, ipc_request: IpcRequest) => $PromiseMaybe<RES | IpcResponse>;
}

export interface $RequestCommonHanlderSchema<I extends $Schema1, O extends $Schema2>
  extends $RequestHanlderSchema<$Schema1ToType<I>, $Schema2ToType<O>> {
  readonly input: I;
  readonly output: O;
}

export interface $RequestCustomHanlderSchema<ARGS = unknown, RES = unknown> extends $RequestHanlderSchema<ARGS, RES> {
  readonly input: (request: IpcRequest) => ARGS;
  readonly output: (request: IpcRequest, result: RES, ipc: Ipc) => $PromiseMaybe<IpcResponse>;
}
