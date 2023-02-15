import type { Remote } from "comlink";
import { transfer } from "comlink";
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
import {
  createResolveTo,
  resolveToRootFile,
} from "../../helper/createResolveTo.cjs";
import { createSignal } from "../../helper/createSignal.cjs";
import {
  $NativeWindow,
  openNativeWindow,
} from "../../helper/openNativeWindow.cjs";
import type { $PromiseMaybe } from "../../helper/types.cjs";
import { parseUrl } from "../../helper/urlHelper.cjs";
import { createHttpDwebServer } from "../http-server/$listenHelper.cjs";
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
      hanlder: (parsed_url: URL) => $PromiseMaybe<$Code>;
    }> = []
  ) {}

  link(url: string) {
    const parsed_url = new URL(url, this.origin);
    for (const item of this.importMaps) {
      if ($isMatchReq(item.pathMatcher, parsed_url.pathname)) {
        return item.hanlder(parsed_url);
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
  ipc.postMessage(
    IpcResponse.fromText(requestMessage.req_id, statusCode, errorMessage)
  );
};

/** 响应实体内容 */
const _ipcSuccessResponse = (
  requestMessage: IpcRequest,
  ipc: Ipc,
  code: $Code
) => {
  const headers = new IpcHeaders({ "Content-Type": code.mime });
  ipc.postMessage(
    typeof code.data === "string"
      ? IpcResponse.fromText(requestMessage.req_id, 200, code.data, headers)
      : IpcResponse.fromBinary(
          requestMessage.req_id,
          200,
          code.data,
          headers,
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
    _ipcErrorResponse(request, ipc, 404, "No Found");
  } else {
    _ipcSuccessResponse(request, ipc, code);
  }
};

/**
 * 将指定的js运行在后台的一个管理器，
 * 注意它们共享一个域，所以要么就关闭
 */
export class JsProcessNMM extends NativeMicroModule {
  override mmid = `js.sys.dweb` as const;
  private nww?: $NativeWindow;
  private _on_shutdown_signal = createSignal<() => unknown>();

  async _bootstrap() {
    const webServer = await createHttpDwebServer(this, {});
    (await webServer.start()).onRequest(async (request, ipc) => {
      ipc.postMessage(
        await IpcResponse.fromResponse(
          request.req_id,
          await this.fetch(
            resolveToRootFile("bundle/js-process" + request.parsed_url.pathname)
          ),
          ipc
        )
      );
    });
    this._on_shutdown_signal.listen(webServer.close);

    const {
      origin: internal_origin,
      start: internal_start,
      close: internal_close,
    } = await createHttpDwebServer(this, {
      subdomain: "internal",
    });

    const JS_PROCESS_WORKER_CODE = await this.fetch(
      resolveToRootFile("bundle/js-process.worker.js")
    ).text();
    /**
     * 内部的代码
     */
    const internal_importLinker = new ImportLinker(internal_origin, [
      {
        pathMatcher: {
          pathname: "/bootstrap.js",
          matchMode: "full",
        },
        hanlder(url) {
          return {
            mime: "application/javascript",
            data: JS_PROCESS_WORKER_CODE,
          };
        },
      },
    ]);
    (await internal_start()).onRequest(
      async (request, internal_httpServerIpc) => {
        void _ipcResponseFromImportLinker(
          internal_httpServerIpc,
          internal_importLinker,
          request
        );
      }
    );
    this._on_shutdown_signal.listen(internal_close);

    const nww = (this.nww = await openNativeWindow(
      webServer.origin + "/index.html",
      {
        /// 如果起始界面是html，说明是调试模式，那么这个窗口也一同展示
        show: require.main?.filename.endsWith(".html"),
      }
    ));

    const apis = nww.getApis<$APIS>();
    /// 创建 web worker
    this.registerCommonIpcOnMessageHanlder({
      method: "POST",
      pathname: "/create-process",
      matchMode: "full",
      input: { main_pathname: "string" },
      output: "object",
      hanlder: (args, ipc, requestMessage) => {
        return this.createProcessAndRun(
          ipc,
          apis,
          `${internal_importLinker.origin}/bootstrap.js?mmid=${ipc.remote.mmid}`,
          args.main_pathname,
          requestMessage
        );
      },
    });
    /// 创建 web 通讯管道
    this.registerCommonIpcOnMessageHanlder({
      pathname: "/create-ipc",
      matchMode: "full",
      input: { process_id: "number" },
      output: "number",
      hanlder: async (args) => {
        const port2 = await apis.createIpc(args.process_id);
        return saveNative2JsIpcPort(port2);
      },
    });
  }
  async _shutdown() {
    this.nww?.close();
    this.nww = undefined;

    this._on_shutdown_signal.emit();
  }

  private async createProcessAndRun(
    ipc: Ipc,
    apis: Remote<$APIS>,
    bootstrap_url: string,
    main_pathname = "/index.js",
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
    void streamIpc.bindIncomeStream(requestMessage.stream());

    /**
     * 让远端提供 esm 模块代码
     * 这里我们将请求转发给对方，要求对方以一定的格式提供代码回来，
     * 我们会对回来的代码进行处理，然后再执行
     */
    const importLinker = new ImportLinker(httpDwebServer.origin, [
      {
        pathMatcher: {
          pathname: "/",
          matchMode: "prefix",
        },
        async hanlder(url) {
          /// TODO 对代码进行翻译处理
          const response = await streamIpc.request(url.href);

          return {
            /// TODO 默认只是js，未来会支持 WASM/JSON 等模块
            mime: "application/javascript",
            data: await response.text(),
          };
        },
      },
    ]);

    (await httpDwebServer.start()).onRequest((request, ipc) => {
      void _ipcResponseFromImportLinker(ipc, importLinker, request);
    });

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
      IPC_ROLE.CLIENT,
      false
    );
    /// 收到 Worker 的数据请求，由 js-process 代理转发出去，然后将返回的内容再代理响应会去
    ipc_to_worker.onRequest(async (ipcMessage, worker_ipc) => {
      const response = await ipc.remote.fetch(ipcMessage.url, ipcMessage);
      worker_ipc.postMessage(
        await IpcResponse.fromResponse(ipcMessage.req_id, response, worker_ipc)
      );
    });

    // this.processImportsMap.set(host, processImports);

    /**
     * 开始执行代码
     */
    await apis.runProcessMain(processInfo.process_id, {
      main_url: parseUrl(main_pathname, httpDwebServer.origin).href,
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

    return new Response(streamIpc.stream, { status: 200 });
  }

  // static singleton = once(() => new JsProcessManager());
}
