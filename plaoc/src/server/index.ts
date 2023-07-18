import { X_PLAOC_QUERY } from "./const.ts";
import {
  $IpcResponse,
  IpcEvent,
  jsProcess,
  PromiseOut,
  queue,
} from "./deps.ts";
import { Server_api } from "./http-api-server.ts";
import { Server_external } from "./http-external-server.ts";
import { Server_www } from "./http-www-server.ts";
import "./polyfill.ts";

import { cors } from "./http-helper.ts";
import {
  all_webview_status,
  mwebview_activate,
  mwebview_open,
  sync_mwebview_status,
} from "./mwebview-helper.ts";

export const main = async () => {
  /**
   * 启动主页面的地址
   */
  const indexUrlPo = new PromiseOut<string>();

  /**
   * 尝试打开gui，或者激活窗口
   */
  const tryOpenView = queue(async () => {
    /// 等待http服务启动完毕，获得入口url
    const url = await indexUrlPo.promise;
    console.log("waterxxbang onActivity🤠",url)
    if (all_webview_status.size === 0) {
      await sync_mwebview_status();
      await mwebview_open(url);
    } else {
      await mwebview_activate();
    }
  });
  /// 立刻自启动
  tryOpenView();
  /// 如果有人来激活，那我就唤醒我的界面
  jsProcess.onActivity(async (_ipcEvent, ipc) => {
    console.log("waterxxbang onActivity🎃",all_webview_status.size)
    await tryOpenView();
    // todo lifecycle 等待加载全部加载完成，再触发ready
    ipc.postMessage(IpcEvent.fromText("ready", "activity"));
  });

  //#region 启动http服务
  const wwwServer = new Server_www();
  const externalServer = new Server_external();
  const apiServer = new Server_api();
  void wwwServer.start();
  void externalServer.start();
  void apiServer.start();

  // 接收外部的请求（接收别的app的请求）
  jsProcess.onRequest(async (ipcRequest, ipc) => {
    // 5秒超时,则认为用户前端没有监听任何外部请求
    const timeOut = setTimeout(() => {
      ipc.postMessage(IpcEvent.fromText("Not found", "The target app is not listening for any requests"));
      externalServer.waitListener.reject();
    }, 5000);
    // 等待监听建立
    await externalServer.waitListener.promise;
    clearTimeout(timeOut);
    // 别的app发送消息，触发一下前端注册的fetch
    externalServer.fetchSignal.emit(ipcRequest);
    const awaitResponse = new PromiseOut<$IpcResponse>();
    externalServer.responseMap.set(ipcRequest.req_id, awaitResponse);
    // 等待 action=response 的返回
    const ipcResponse = await awaitResponse.promise;
    cors(ipcResponse.headers);
    // 返回数据到发送者那边
    return ipc.postMessage(ipcResponse);
  });

  /// 生成 index-url
  {
    const wwwStartResult = await wwwServer.getStartResult();
    const apiStartResult = await apiServer.getStartResult();
    const indexUrl = wwwStartResult.urlInfo.buildInternalUrl((url) => {
      url.pathname = "/index.html";
      url.searchParams.set(
        X_PLAOC_QUERY.API_INTERNAL_URL,
        apiStartResult.urlInfo.buildInternalUrl().href
      );
      url.searchParams.set(
        X_PLAOC_QUERY.API_PUBLIC_URL,
        apiStartResult.urlInfo.buildPublicUrl().href
      );
      url.searchParams.set(X_PLAOC_QUERY.EXTERNAL_URL, externalServer.token);
    });
    console.log("open in browser:", indexUrl.href);
    indexUrlPo.resolve(indexUrl.href);
  }
  //#endregion
};

main();
