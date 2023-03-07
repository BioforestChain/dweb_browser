import type { Remote } from "comlink";
import { transfer } from "comlink";
import { once } from "lodash";
import { MessagePortIpc } from "../../core/ipc-web/MessagePortIpc.cjs";
import { ReadableStreamIpc } from "../../core/ipc-web/ReadableStreamIpc.cjs";
import {
  Ipc,
  IpcRequest,
  IpcResponse,
  IPC_ROLE,
} from "../../core/ipc/index.cjs";
import { IpcHeaders } from "../../core/ipc/IpcHeaders.cjs";
import { NativeMicroModule } from "../../core/micro-module.native.cjs";
import { $isMatchReq, $ReqMatcher } from "../../helper/$ReqMatcher.cjs";
import { createResolveTo } from "../../helper/createResolveTo.cjs";
import { mapHelper } from "../../helper/mapHelper.cjs";
import { openNativeWindow } from "../../helper/openNativeWindow.cjs";
import { PromiseOut } from "../../helper/PromiseOut.cjs";
import type { $PromiseMaybe } from "../../helper/types.cjs";
import { createHttpDwebServer } from "../http-server/$createHttpDwebServer.cjs";
import { saveNative2JsIpcPort } from "./ipc.native2js.cjs";

const resolveTo = createResolveTo(__dirname);

// @ts-ignore
type $APIS = typeof import("./assets/js-process.web.mjs")["APIS"];

class ImportLinker {
  constructor(
    readonly origin: string,
    /** 我们将托管用户的代码，响应虚拟环境中的 import 请求 */
    readonly importMaps: Array<{
      pathMatcher: $ReqMatcher;
      handler: (parsed_url: URL) => $PromiseMaybe<$Code>;
    }> = []
  ) {}

  link(url: string) {
    const parsed_url = new URL(url, this.origin);
    for (const item of this.importMaps) {
      if ($isMatchReq(item.pathMatcher, parsed_url.pathname)) {
        return item.handler(parsed_url);
      }
    }
  }
}

interface $Code {
  mime: string;
  data: Uint8Array | string;
}

/** 响应错误信息 */
const _ipcErrorResponse = (
  requestMessage: IpcRequest,
  ipc: Ipc,
  statusCode: number,
  errorMessage: string
) => {
  const headers = new IpcHeaders(CORS_HEADERS);
  ipc.postMessage(
    IpcResponse.fromText(
      requestMessage.req_id,
      statusCode,
      headers,
      errorMessage,
      ipc
    )
  );
};

/** 响应实体内容 */
const _ipcSuccessResponse = (
  requestMessage: IpcRequest,
  ipc: Ipc,
  code: $Code
) => {
  const headers = new IpcHeaders(CORS_HEADERS);
  headers.set("Content-Type", code.mime);
  ipc.postMessage(
    typeof code.data === "string"
      ? IpcResponse.fromText(
          requestMessage.req_id,
          200,
          headers,
          code.data,
          ipc
        )
      : IpcResponse.fromBinary(
          requestMessage.req_id,
          200,
          headers,
          code.data,
          ipc
        )
  );
};

/** 基于 importLinker 响应内容 */
const _ipcResponseFromImportLinker = async (
  ipc: Ipc,
  importLinker: ImportLinker,
  request: IpcRequest
) => {
  const code = await importLinker.link(request.url);
  if (code === undefined) {
    _ipcErrorResponse(request, ipc, 404, "// No Found");
  } else {
    _ipcSuccessResponse(request, ipc, code);
  }
};
const CORS_HEADERS = [
  ["Content-Type", "application/javascript"],
  ["Access-Control-Allow-Origin", "*"],
  ["Access-Control-Allow-Headers", "*"], // 要支持 X-Dweb-Host
  ["Access-Control-Allow-Methods", "*"],
] satisfies HeadersInit;

/**
 * 将指定的js运行在后台的一个管理器，
 * 注意它们共享一个域，所以要么就关闭
 */
export class JsProcessNMM extends NativeMicroModule {
  override mmid = `js.sys.dweb` as const;

  private JS_PROCESS_WORKER_CODE = once(() => {
    return this.nativeFetch("file:///bundle/js-process.worker.js").text();
  });

  private INTERNAL_PATH = encodeURI("/<internal>");

  async _bootstrap() {
    const mainServer = await createHttpDwebServer(this, {});
    (await mainServer.listen()).onRequest(async (request, ipc) => {
      ipc.postMessage(
        await IpcResponse.fromResponse(
          request.req_id,
          await this.nativeFetch(
            "file:///bundle/js-process" + request.parsed_url.pathname
          ),
          ipc
        )
      );
    });
    this._after_shutdown_signal.listen(mainServer.close);

    const apis = await (async () => {
      const urlInfo = mainServer.startResult.urlInfo;
      const nww = await openNativeWindow(
        mainServer.startResult.urlInfo.buildInternalUrl((url) => {
          url.pathname = "/index.html";
        }).href,
        {
          /// 如果起始界面是html，说明是调试模式，那么这个窗口也一同展示
          show: require.main?.filename.endsWith(".html"),
        },
        { userAgent: (userAgent) => userAgent + ` dweb-host/${urlInfo.host}` }
      );
      this._after_shutdown_signal.listen(() => {
        nww.close();
      });
      return nww.getApis<$APIS>();
    })();

    const ipcProcessIdMap = new WeakMap<Ipc, Map<string, PromiseOut<number>>>();

    /// 创建 web worker
    this.registerCommonIpcOnMessageHandler({
      method: "POST",
      pathname: "/create-process",
      matchMode: "full",
      input: { entry: "string", process_id: "string" },
      output: "object",
      handler: async (args, ipc, requestMessage) => {
        const processIdMap = mapHelper.getOrPut(
          ipcProcessIdMap,
          ipc,
          () => new Map()
        );

        if (processIdMap.has(args.process_id)) {
          throw new Error(
            `ipc:${ipc.remote.mmid}/processId:${args.process_id} has already using`
          );
        }
        const po = new PromiseOut<number>();
        processIdMap.set(args.process_id, po);

        const result = await this.createProcessAndRun(
          ipc,
          apis,
          args.entry,
          requestMessage
        );
        po.resolve(result.processInfo.process_id);
        return result.streamIpc.stream;
      },
    });
    /// 创建 web 通讯管道
    this.registerCommonIpcOnMessageHandler({
      pathname: "/create-ipc",
      matchMode: "full",
      input: { process_id: "string" },
      output: "number",
      handler: async (args, ipc) => {
        const process_id_po = ipcProcessIdMap.get(ipc)?.get(args.process_id);
        if (process_id_po === undefined) {
          throw new Error(
            `ipc:${ipc.remote.mmid}/processId:${args.process_id} invalid`
          );
        }
        const process_id = await process_id_po.promise;
        const port_id = await this.createIpc(ipc, apis, process_id);
        return port_id;
      },
    });
  }
  async _shutdown() {}

  private async createProcessAndRun(
    ipc: Ipc,
    apis: Remote<$APIS>,
    entry = "/index.js",
    requestMessage: IpcRequest
  ) {
    /**
     * 用自己的域名的权限为它创建一个子域名
     */
    const httpDwebServer = await createHttpDwebServer(this, {
      subdomain: ipc.remote.mmid,
    });

    /**
     * 远端是代码服务，所以这里是 client 的身份
     */
    const streamIpc = new ReadableStreamIpc(ipc.remote, IPC_ROLE.CLIENT);
    void streamIpc.bindIncomeStream(requestMessage.body.stream());

    /**
     * 让远端提供 esm 模块代码
     * 这里我们将请求转发给对方，要求对方以一定的格式提供代码回来，
     * 我们会对回来的代码进行处理，然后再执行
     */
    const importLinker = new ImportLinker(
      httpDwebServer.startResult.urlInfo.internal_origin,
      [
        {
          pathMatcher: {
            pathname: "/",
            matchMode: "prefix",
          },
          handler: async (url) => {
            // <internal>开头的是特殊路径：交由内部处理，不会推给远端处理
            if (url.pathname.startsWith(this.INTERNAL_PATH)) {
              url.pathname = url.pathname.substring(this.INTERNAL_PATH.length);
              if (url.pathname === "/bootstrap.js") {
                return {
                  mime: "application/javascript",
                  data: await this.JS_PROCESS_WORKER_CODE(),
                };
              }
            }

            /// TODO 对代码进行翻译处理
            const response = await streamIpc.request(url.href);

            return {
              /// TODO 默认只是js，未来会支持 WASM/JSON 等模块
              mime: "application/javascript",
              data: await response.body.text(),
            };
          },
        },
      ]
    );

    (await httpDwebServer.listen()).onRequest((request, ipc) => {
      void _ipcResponseFromImportLinker(ipc, importLinker, request);
    });

    const bootstrap_url = httpDwebServer.startResult.urlInfo.buildInternalUrl(
      (url) => {
        url.pathname = `${this.INTERNAL_PATH}/bootstrap.js`;
        url.searchParams.set("mmid", ipc.remote.mmid);
        url.searchParams.set("host", httpDwebServer.startResult.urlInfo.host);
        /// 这里是 nodejs 和 web-browser 的通讯，electorn 提供了 raw 的支持
        url.searchParams.append("ipc-support-protocols", "raw");
        url.searchParams.append("ipc-support-protocols", "message_pack");
      }
    ).href;

    /**
     * 创建一个通往 worker 的消息通道
     */
    const channel_for_worker = new MessageChannel();
    const processInfo = await apis.createProcess(
      bootstrap_url,
      transfer(channel_for_worker.port2, [channel_for_worker.port2])
    );

    /**
     * 将 js-worker 中的请求进行中转代理
     * 在Android和IOS中的Webview默认只能传输字符串
     */
    const ipc_to_worker = new MessagePortIpc(
      channel_for_worker.port1,
      ipc.remote,
      IPC_ROLE.CLIENT
    );
    /// 收到 Worker 的数据请求，由 js-process 代理转发出去，然后将返回的内容再代理响应会去
    ipc_to_worker.onRequest(async (ipcMessage, worker_ipc) => {
      const response = await ipc.remote.nativeFetch(ipcMessage.toRequest());
      worker_ipc.postMessage(
        await IpcResponse.fromResponse(ipcMessage.req_id, response, worker_ipc)
      );
    });

    // this.processImportsMap.set(host, processImports);

    /**
     * 开始执行代码
     */
    await apis.runProcessMain(processInfo.process_id, {
      main_url: httpDwebServer.startResult.urlInfo.buildInternalUrl((url) => {
        url.pathname = entry;
      }).href,
    });

    /// 绑定销毁
    /**
     * “模块之间的IPC通道”关闭的时候，关闭“代码IPC流通道”
     *
     * > 自己shutdown的时候，这些ipc会被关闭
     */
    ipc.onClose(() => {
      streamIpc.close();
    });
    /**
     * “代码IPC流通道”关闭的时候，关闭这个子域名
     */
    streamIpc.onClose(() => {
      httpDwebServer.close();
    });
    return {
      streamIpc,
      processInfo,
    };
  }

  private async createIpc(ipc: Ipc, apis: Remote<$APIS>, process_id: number) {
    /**
     * 创建一个通往 worker 的消息通道
     */
    const channel_for_worker = new MessageChannel();
    await apis.createIpc(
      process_id,
      transfer(channel_for_worker.port2, [channel_for_worker.port2])
    );
    return saveNative2JsIpcPort(channel_for_worker.port1);
  }
  // static singleton = once(() => new JsProcessManager());
}
