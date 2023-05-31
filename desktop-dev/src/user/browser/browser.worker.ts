import { DetailedDiff, detailedDiff } from "deep-object-diff";
import type { IpcResponse } from "../../core/ipc/IpcResponse.ts";
import { PromiseOut } from "../../helper/PromiseOut.ts";
import { createSignal } from "../../helper/createSignal.ts";
import { closeApp, webViewMap } from "../tool/app.handle.ts";
import { EVENT, WebViewState, AllWebviewState } from "../tool/tool.event.ts";
import {
  closeDwebView,
  closeWindow,
  cros,
  nativeActivate,
  nativeOpen,
} from "../tool/tool.native.ts";
import { $Ipc, fetchSignal } from "../tool/tool.request.ts";
// 测试用
import { onApiRequest } from "../tool/tool.request.ts"
import { IpcRequest } from "../../../electron/script/core/ipc/IpcRequest.js";
// import { onApiRequest } from "./api.request.ts";
 

const main = async () => {
  const { IpcEvent } = ipc;
  // 启动主页面的地址
  const mainUrl = new PromiseOut<string>();
  // 管理webview的状态，因为当前webview是通过状态判断操作的，比如激活，关闭
  let oldWebviewState: AllWebviewState = {};
  // 跟multiWebView 建立连接
  const multiWebViewIpc = await jsProcess.connect("mwebview.sys.dweb");
  // 关闭信号
  const multiWebViewCloseSignal = createSignal<() => unknown>();
  const EXTERNAL_PREFIX = "/external/";
  const { IpcResponse } = ipc;
  const externalMap = new Map<number, PromiseOut<IpcResponse>>();

  /**尝试打开view */
  // tryOpenView 是属于开发者自己写的内容
  const tryOpenView = async () => {
    console.log('tryOpenView... start')
    const url = await mainUrl.promise;
    nativeOpen(url);

    console.log('tryOpenView... end', url)
    // if (webViewMap.size === 0) {
    //   // open
    //   const url = await mainUrl.promise;
    //   const view_id = await nativeOpen(url);
    //   webViewMap.set(view_id, {
    //     isActivated: true,
    //     webviewId: view_id,
    //   });
    //   return view_id;
    // }
    // // 当前的策略是有多少个webview激活多少个
    // await Promise.all(
    //   [...webViewMap.values()].map((item) => {
    //     // activate
    //     return nativeActivate(item.webviewId);
    //   })
    // );
  };

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
  // 别滴app发送到请求走这里发送到前端的DwebServiceWorker fetch
  const externalReadableStreamIpc = await externalServer.listen();

  apiReadableStreamIpc.onRequest(async (request, ipc) => {
    const url = request.parsed_url;
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
      `file:///sys/cot-demo${pathname}?mode=stream`
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
    const url = request.parsed_url;
    const xHost = decodeURIComponent(url.searchParams.get("X-Dweb-Host") ?? "");

    // 处理serviceworker respondWith过来的请求,回复给别的app
    if (url.pathname.startsWith(EXTERNAL_PREFIX)) {
      const pathname = url.pathname.slice(EXTERNAL_PREFIX.length);
      const externalReqId = parseInt(pathname);
      // 验证传递的reqId
      if (typeof externalReqId !== "number" || isNaN(externalReqId)) {
        return ipc.postMessage(
          IpcResponse.fromText(
            request.req_id,
            400,
            request.headers,
            "reqId is NAN",
            ipc
          )
        );
      }
      const responsePOo = externalMap.get(externalReqId);
      // 验证是否有外部请求
      if (!responsePOo) {
        return ipc.postMessage(
          IpcResponse.fromText(
            request.req_id,
            500,
            request.headers,
            `not found external requst,req_id ${externalReqId}`,
            ipc
          )
        );
      }
      // 转发给外部的app
      responsePOo.resolve(
        new IpcResponse(externalReqId, 200, request.headers, request.body, ipc)
      );
      externalMap.delete(externalReqId);
      const icpResponse = IpcResponse.fromText(
        request.req_id,
        200,
        request.headers,
        "ok",
        ipc
      );
      cros(icpResponse.headers);
      // 告知自己的 respondWith 已经发送成功了
      return ipc.postMessage(icpResponse);
    }

    // 别的app发送消息，触发一下前端注册的fetch
    if (xHost === externalServer.startResult.urlInfo.host) {
      fetchSignal.emit(request);
      const awaitResponse = new PromiseOut<IpcResponse>();
      externalMap.set(request.req_id, awaitResponse);
      const ipcResponse = await awaitResponse.promise;
      cros(ipcResponse.headers);
      // 返回数据到发送者那边
      ipc.postMessage(ipcResponse);
    }
  });

  // 转发serviceWorker 请求
  const serviceWorkerFactory = async (url: URL, ipc: $Ipc) => {
    const pathname = url.pathname;
    // 关闭的流程需要调整
    // 向dns发送关闭当前 模块的消息
    // woker.js -> dns -> JsMicroModule -> woker.js -> 其他的 NativeMicroModule

    if (pathname.endsWith("restart")) {
      // 关闭全部的服务
      await apiServer.close();
      await wwwServer.close();
      await externalServer.close();

      // 关闭全部的ipc
      apiReadableStreamIpc.close();
      wwwReadableStreamIpc.close();
      externalReadableStreamIpc.close();

      jsProcess.nativeFetch(
        `file://dns.sys.dweb/restart?app_id=${jsProcess.mmid}`
      )
      return "restart ok"
    }

    // 只关闭 渲染一个渲染进程 不关闭 service
    if(pathname.endsWith('close')){
      closeWindow()
      return `result ok`;
    }
    return "no action for serviceWorker Factory !!!";
  };
  
  /// 如果有人来激活，那我就唤醒我的界面
  jsProcess.onActivity(async (_ipcEvent, ipc) => {
    await tryOpenView();
    ipc.postMessage(IpcEvent.fromText("ready", "activity"));
    // if (hasActivityEventIpcs.has(ipc) === false) {
    //   hasActivityEventIpcs.add(ipc);
    //   multiWebViewCloseSignal.listen(() => {
    //     ipc.postMessage(IpcEvent.fromText("close", ""));
    //     ipc.close();
    //   });
    // }
  });
  const hasActivityEventIpcs = new Set<$Ipc>();
  jsProcess.onClose(() => {
    closeWindow()
    // 接收JMM更新程序的关闭消息（安装完新的app需要重启应用）
    // multiWebViewCloseSignal.emit();
    // return closeApp(
    //   [apiServer, wwwServer, externalServer],
    //   [apiReadableStreamIpc, wwwReadableStreamIpc, externalReadableStreamIpc]
    // );
  });

  /// 同步 mwebview 的状态机
  multiWebViewIpc.onEvent((event, ipc) => {
    console.log('allWebveiwState: ', event.data)
    console.error('这里需要 同 android 协调 传递过来的 allWebview 数据数据类型')
    // if (event.name === EVENT.State && typeof event.data === "string") {
    //   const newState = JSON.parse(event.data);
    //   // newState 的数据格式是 {key: string}
    //   const diff = detailedDiff(oldWebviewState, newState);
    //   oldWebviewState = newState;
    //   diffFactory(diff);
      
    // }
    // multiWebViewCloseSignal.listen(() => {
    //   ipc.postMessage(IpcEvent.fromText("close", ""));
    //   ipc.close();
    // });
  });

  const diffFactory = async (diff: DetailedDiff) => {
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

  const interUrl = wwwServer.startResult.urlInfo.buildInternalUrl((url) => {
    url.pathname = "/index.html";
  });
  interUrl.searchParams.set(
    "X-Plaoc-Internal-Url",
    apiServer.startResult.urlInfo.buildInternalUrl().href
  );
  interUrl.searchParams.set(
    "X-Plaoc-Public-Url",
    apiServer.startResult.urlInfo.buildPublicUrl().href
  );
  mainUrl.resolve(interUrl.href);

  /**
   * 立刻自启动
   */
  tryOpenView();
};

setTimeout(main, 1000);
// main();
