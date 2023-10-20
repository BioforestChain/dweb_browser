import "dweb/core/helper/crypto.shims.ts";
import isMobile from "npm:is-mobile";
import { X_PLAOC_QUERY } from "./const.ts";
import { jsProcess, PromiseOut, queue } from "./deps.ts";
import { Server_api } from "./http-api-server.ts";
import { Server_external } from "./http-external-server.ts";
import { Server_www } from "./http-www-server.ts";
import "./polyfill.ts";

import { all_webview_status, mwebview_activate, mwebview_open, sync_mwebview_status } from "./mwebview-helper.ts";
import { PlaocConfig } from "./plaoc-config.ts";

export const main = async () => {
  /**
   * 启动主页面的地址
   */
  const indexUrlPo = new PromiseOut<string>();
  let widPo = new PromiseOut<string>();
  /**
   * 尝试打开gui，或者激活窗口
   */
  const tryOpenView = queue(async () => {
    /// 等待http服务启动完毕，获得入口url
    const url = await indexUrlPo.promise;
    if (all_webview_status.size === 0) {
      await sync_mwebview_status();
      console.log("mwebview_open=>", url);
      if (widPo.is_resolved) {
        apiServer.widPo = widPo = new PromiseOut();
      }
      const { wid } = await mwebview_open(url);
      widPo.resolve(wid);
    } else {
      console.log("mwebview_activate=>", url);
      await mwebview_activate();
    }
  });
  /// 如果有人来激活，那我就唤醒我的界面
  jsProcess.onActivity(async (_ipcEvent) => {
    console.log(`${jsProcess.mmid} onActivity`);
    tryOpenView();
  });

  jsProcess.onClose(() => {
    console.log("app后台被关闭。");
  });

  //#region 启动http服务
  const plaocConfig = await PlaocConfig.init();
  const wwwServer = new Server_www(plaocConfig);
  const externalServer = new Server_external();
  const apiServer = new Server_api(widPo);
  const wwwListenerTask = wwwServer.start().finally(() => console.log("wwwServer started"));
  const externalListenerTask = externalServer.start().finally(() => console.log("externalServer started"));
  const apiListenerTask = apiServer.start().finally(() => console.log("apiServer started"));

  all_webview_status.signal.listen((size) => {
    if (size === 0) {
      externalServer.closeRegisterIpc();
    }
  });

  /// 生成 index-url
  const wwwStartResult = await wwwServer.getStartResult();
  const apiStartResult = await apiServer.getStartResult();
  const usePublic =
    plaocConfig.config.usePublicUrl ??
    (isMobile.isMobile() ? (navigator.userAgent.includes("Android") ? false : true) : true);
  const indexUrl = wwwStartResult.urlInfo.buildHtmlUrl(usePublic, (url) => {
    url.pathname = "/index.html";
    url.searchParams.set(X_PLAOC_QUERY.API_INTERNAL_URL, apiStartResult.urlInfo.buildUrl(usePublic).href);
    url.searchParams.set(X_PLAOC_QUERY.API_PUBLIC_URL, apiStartResult.urlInfo.buildPublicUrl().href);
    url.searchParams.set(X_PLAOC_QUERY.EXTERNAL_URL, externalServer.token);
  });
  console.log("open in browser:", indexUrl.href,usePublic);
  await Promise.all([wwwListenerTask, externalListenerTask, apiListenerTask]);
  indexUrlPo.resolve(indexUrl.href);
  tryOpenView();
  //#endregion
};

main();
