import { X_PLAOC_QUERY } from "./const.ts";
import { IpcEvent, jsProcess, PromiseOut, queue } from "./deps.ts";
import { Server_api } from "./http-api-server.ts";
import { Server_external } from "./http-external-server.ts";
import { Server_www } from "./http-www-server.ts";

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
    if (all_webview_status.size === 0) {
      await sync_mwebview_status();
      await mwebview_open(url);
    } else {
      await mwebview_activate();
    }
  });
  /// 如果有人来激活，那我就唤醒我的界面
  jsProcess.onActivity(async (_ipcEvent, ipc) => {
    await tryOpenView();
    ipc.postMessage(IpcEvent.fromText("ready", "activity"));
  });
  /// 立刻自启动
  tryOpenView();

  //#region 启动http服务
  const wwwServer = new Server_www();
  const externalServer = new Server_external();
  const apiServer = await new Server_api();
  void wwwServer.start();
  void externalServer.start();
  void apiServer.start(
    await wwwServer.getServer(),
    await externalServer.getServer()
  );

  /// 生成 index-url
  {
    const wwwStartResult = await wwwServer.getStartResult();
    const apiStartResult = await apiServer.getStartResult();
    const indexUrl = wwwStartResult.urlInfo.buildInternalUrl((url) => {
      url.pathname = "/index.html";
      url.searchParams.set(
        X_PLAOC_QUERY.INTERNAL_URL,
        apiStartResult.urlInfo.buildInternalUrl().href
      );
      url.searchParams.set(
        X_PLAOC_QUERY.PUBLIC_URL,
        apiStartResult.urlInfo.buildPublicUrl().href
      );
    });
    indexUrlPo.resolve(indexUrl.href);
  }
  //#endregion
};

main();
