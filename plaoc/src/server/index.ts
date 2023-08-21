import isMobile from "npm:is-mobile";
import { X_PLAOC_QUERY } from "./const.ts";
import { $IpcRequest, IpcEvent, IpcResponse, jsProcess, PromiseOut, queue } from "./deps.ts";
import { Server_api } from "./http-api-server.ts";
import { ExternalState, Server_external } from "./http-external-server.ts";
import { Server_www } from "./http-www-server.ts";
import "./polyfill.ts";

import { urlStore } from "./helper/urlStore.ts";
import { cors } from "./http-helper.ts";
import { all_webview_status, mwebview_activate, mwebview_open, sync_mwebview_status } from "./mwebview-helper.ts";

export const main = async () => {
  /**
   * 启动主页面的地址
   */
  const indexUrlPo = new PromiseOut<string>();
  const widPo = new PromiseOut<string>();

  /**
   * 尝试打开gui，或者激活窗口
   */
  const tryOpenView = queue(async () => {
    /// 等待http服务启动完毕，获得入口url
    const url = await indexUrlPo.promise;
    if (all_webview_status.size === 0) {
      await sync_mwebview_status();
      const { wid } = await mwebview_open(url);
      widPo.resolve(wid);
    } else {
      await mwebview_activate();
    }
  });
  /// 如果有人来激活，那我就唤醒我的界面
  jsProcess.onActivity(async (ipcEvent, ipc) => {
    console.log(`${jsProcess.mmid} onActivity=>`, ipcEvent.data);
    // 对方主动发送过来的关闭请求，会中断对方的等待
    if (ipcEvent.data === ExternalState.CLOSE) {
      return externalServer.waitListener.resolve(false);
    }
    tryOpenView();
    // 等待监听建立- 此处的请求会交给开发者控制
    if (ipcEvent.data === ExternalState.CONNECT_AWAIT) {
      //如果对方没有设置监听将会一直等待
      const bool = await externalServer.waitListener.promise;
      const state = bool ? ExternalState.CONNECT_OK : ExternalState.CLOSE;
      return ipc.postMessage(IpcEvent.fromText(state, state));
    }
  });

  //#region 启动http服务
  const wwwServer = new Server_www();
  const externalServer = new Server_external();
  const apiServer = new Server_api(widPo);
  void wwwServer.start();
  void externalServer.start();
  void apiServer.start();

  // 接收外部的请求（接收别的app的请求）
  jsProcess.onRequest(async (ipcRequest, ipc) => {
    // 别的app发送消息，触发一下前端注册的fetch
    externalServer.fetchSignal.emit(ipcRequest);
    const request = new PromiseOut<$IpcRequest>();
    externalServer.responseMap.set(ipcRequest.req_id, request);
    // 等待 action=response 的返回
    const data = await request.promise;
    console.log("response=>", data);
    // 返回数据到发送者那边
    return ipc.postMessage(new IpcResponse(ipcRequest.req_id, 200, cors(data.headers), data.body, ipc));
  });

  /// 生成 index-url
  {
    const wwwStartResult = await wwwServer.getStartResult();
    const apiStartResult = await apiServer.getStartResult();
    const usePublic = isMobile.isMobile();
    const indexUrl = wwwStartResult.urlInfo.buildHtmlUrl(usePublic, (url) => {
      url.pathname = "/index.html";
      urlStore.set({
        [X_PLAOC_QUERY.API_INTERNAL_URL]: apiStartResult.urlInfo.buildUrl(usePublic).href,
        [X_PLAOC_QUERY.API_PUBLIC_URL]: apiStartResult.urlInfo.buildPublicUrl().href,
        [X_PLAOC_QUERY.EXTERNAL_URL]: externalServer.token,
      });
      url.searchParams.set(X_PLAOC_QUERY.API_INTERNAL_URL, apiStartResult.urlInfo.buildUrl(usePublic).href);
      url.searchParams.set(X_PLAOC_QUERY.API_PUBLIC_URL, apiStartResult.urlInfo.buildPublicUrl().href);
      url.searchParams.set(X_PLAOC_QUERY.EXTERNAL_URL, externalServer.token);
    });
    console.log("open in browser:", indexUrl.href);
    indexUrlPo.resolve(indexUrl.href);
  }
  //#endregion
};

main();
