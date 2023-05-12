import { DetailedDiff, detailedDiff } from "deep-object-diff";
import { PromiseOut } from "../../helper/PromiseOut.mjs";
import { createSignal } from "../../helper/createSignal.mjs";
import { closeFront, restartApp, webViewMap } from "../tool/app.handle.mjs";
import { EVENT, WebViewState } from "../tool/tool.event.mjs";
import {
  closeDwebView,
  cros,
  nativeActivate,
  nativeOpen,
} from "../tool/tool.native.mjs";
import { $Ipc, fetchSignal, onApiRequest } from "../tool/tool.request.mjs";

const main = async () => {
  const { IpcEvent } = ipc;
  // 启动主页面的地址
  const mainUrl = new PromiseOut<string>();
  // 管理webview的状态，因为当前webview是通过状态判断操作的，比如激活，关闭
  let oldWebviewState: WebViewState[] = [];
  // 跟multiWebView 建立连接
  const multiWebViewIpc = await jsProcess.connect("mwebview.sys.dweb");
  // 关闭信号
  const multiWebViewCloseSignal = createSignal<() => unknown>();

  /**尝试打开view */
  const tryOpenView = () => {
    const newWindowState = new PromiseOut<boolean>();
    (async () => {
      try {
        if (webViewMap.size === 0) {
          // open
          const url = await mainUrl.promise;
          const view_id = await nativeOpen(url);
          webViewMap.set(view_id, {
            isActivated: true,
            webviewId: view_id,
          });
          return view_id;
        }
        // 当前的策略是有多少个webview激活多少个
        await Promise.all(
          [...webViewMap.values()].map((item) => {
            // activate
            return nativeActivate(item.webviewId);
          })
        );
      } finally {
        newWindowState.resolve(true);
      }
    })();
    windowState = newWindowState;
    return newWindowState;
  };

  /**
   * 窗口状态
   * 立刻自启动
   */
  let windowState: PromiseOut<boolean>;
  windowState = tryOpenView();

  const { IpcResponse, IpcHeaders } = ipc;

  /**给前端的文件服务 */
  const wwwServer = await http.createHttpDwebServer(jsProcess, {
    subdomain: "www",
    port: 443,
  });

  /**给前端的api服务 */
  const apiServer = await http.createHttpDwebServer(jsProcess, {
    subdomain: "api",
    port: 443,
  });
  /**给前端的api服务 */
  const externalServer = await http.createHttpDwebServer(jsProcess, {
    subdomain: "external",
    port: 443,
  });

  // 自己api处理 Fetch
  const apiReadableStreamIpc = await apiServer.listen();
  // 文件服务处理
  const wwwReadableStreamIpc = await wwwServer.listen();
  // 别人调用处理 onFetch
  const externalReadableStreamIpc = await externalServer.listen();

  apiReadableStreamIpc.onRequest(async (request, ipc) => {
    const url = new URL(
      request.url,
      apiServer.startResult.urlInfo.internal_origin
    );
    // serviceWorker
    if (url.pathname.startsWith("/dns.sys.dweb")) {
      const result = await serviceWorkerFactory(url, ipc);
      const ipcResponse = IpcResponse.fromText(
        request.req_id,
        200,
        undefined,
        result,
        ipc
      );
      cros(ipcResponse.headers);
      // 返回数据到前端
      return ipc.postMessage(ipcResponse);
    }
    onApiRequest(apiServer.startResult.urlInfo, request, ipc);
  });

  wwwReadableStreamIpc.onRequest(async (request, ipc) => {
    let pathname = request.parsed_url.pathname;
    if (pathname === "/") {
      pathname = "/index.html";
    }
    const remoteIpcResponse = await jsProcess.nativeRequest(
      `file:///cot-demo${pathname}?mode=stream`
    );
    /**
     * 流转发，是一种高性能的转发方式，等于没有真正意义上去读取response.body，
     * 而是将response.body的句柄直接转发回去，那么根据协议，一旦流开始被读取，自己就失去了读取权。
     *
     * 如此数据就不会发给我，节省大量传输成本
     */
    ipc.postMessage(
      new IpcResponse(
        request.req_id,
        remoteIpcResponse.statusCode,
        cros(remoteIpcResponse.headers),
        remoteIpcResponse.body,
        ipc
      )
    );
  });
  // 提供APP之间通信的方法
  externalReadableStreamIpc.onRequest(async (request, ipc) => {
    console.log("externalReadableStreamIpc =>", request, ipc);
    // 当用户请求其他APP的时候请求走的是这里
    fetchSignal.emit(request);
    // TODO
    console.log(request, ipc);
  });

  // 转发serviceWorker 请求
  const serviceWorkerFactory = async (url: URL, ipc: $Ipc) => {
    const pathname = url.pathname;
    // 关闭前端
    if (pathname.endsWith("close")) {
      return closeFront();
    }
    // 重启app，伴随着前后端重启
    if (pathname.endsWith("restart")) {
      return restartApp(
        [apiServer, wwwServer],
        [apiReadableStreamIpc, wwwReadableStreamIpc]
      );
    }
    // TODO 手动关闭 connect
    // closeSignal.emit()
    // cotDemoJMM.shutdown()

    // return await response.text()
    return "no action for serviceWorker Factory !!!";
  };

  /// 如果有人来激活，那我就唤醒我的界面
  jsProcess.onActivity(async (ipcEvent, ipc) => {
    if ((await windowState.promise) === false) {
      await tryOpenView();
    }
    ipc.postMessage(IpcEvent.fromText("ready", "activity"));
    if (hasActivityEventIpcs.has(ipc) === false) {
      hasActivityEventIpcs.add(ipc);
      multiWebViewCloseSignal.listen(() => {
        ipc.postMessage(IpcEvent.fromText("close", ""));
        ipc.close();
      });
    }
  });
  const hasActivityEventIpcs = new Set<$Ipc>();

  /// 同步 mwebview 的状态机
  multiWebViewIpc.onEvent(async (event) => {
    console.log("connectMultiWebView =>", event.name);
    if (event.name === EVENT.State && typeof event.data === "string") {
      const newState = JSON.parse(event.data);
      const diff = detailedDiff(oldWebviewState, newState);
      oldWebviewState = newState;
      diffFactory(diff);
    }
  });

  const diffFactory = async (diff: DetailedDiff) => {
    console.log("connectMultiWebView diffFactory=>", diff);
    //  是否有新增
    for (const id in diff.added) {
      webViewMap.set(id, JSON.parse(diff.added[id as keyof typeof diff.added]));
    }
    // 是否有删除
    for (const id in diff.deleted) {
      webViewMap.delete(id);
      await closeDwebView(id);
    }
    // 是否有更新
    for (const id in diff.updated) {
      webViewMap.set(
        id,
        JSON.parse(diff.updated[id as keyof typeof diff.updated])
      );
      await nativeActivate(id);
    }
  };

  {
    const interUrl = wwwServer.startResult.urlInfo.buildInternalUrl((url) => {
      url.pathname = "/index.html";
    }).href;
    mainUrl.resolve(interUrl);
  }
};

main();
