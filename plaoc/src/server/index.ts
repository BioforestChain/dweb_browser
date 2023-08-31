import isMobile from "npm:is-mobile";
import { X_PLAOC_QUERY } from "./const.ts";
import { jsProcess, PromiseOut, queue } from "./deps.ts";
import { Server_api } from "./http-api-server.ts";
import { Server_external } from "./http-external-server.ts";
import { Server_www } from "./http-www-server.ts";
import "./polyfill.ts";

import { urlStore } from "./helper/urlStore.ts";
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
  jsProcess.onActivity(async (_ipcEvent) => {
    tryOpenView();
  });

  //#region 启动http服务
  const wwwServer = new Server_www();
  const externalServer = new Server_external();
  const apiServer = new Server_api(widPo);
  void wwwServer.start();
  void externalServer.start();
  void apiServer.start();

  /// 生成 index-url
  {
    const wwwStartResult = await wwwServer.getStartResult();
    const apiStartResult = await apiServer.getStartResult();
    const usePublic = isMobile.isMobile();
    const indexUrl = wwwStartResult.urlInfo.buildHtmlUrl(false, (url) => {
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
