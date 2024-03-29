import { X_PLAOC_QUERY } from "./const.ts";
import { jsProcess, PromiseOut } from "./deps.ts";
import "./helper/polyfill.ts";
import { Server_api } from "./http-api-server.ts";
import { Server_external } from "./http-external-server.ts";
import { Server_www } from "./http-www-server.ts";
import "./shim/crypto.shims.ts";

import {
  all_webview_status,
  apply_window,
  mwebview_activate,
  mwebview_open,
  open_main_window,
  sync_mwebview_status,
} from "./helper/mwebview-helper.ts";
import { queue } from "./helper/queue.ts";
import { MiddlewareImporter } from "./middleware-importer.ts";
import { PlaocConfig } from "./plaoc-config.ts";

const main = async () => {
  /**
   * 启动主页面的地址
   */
  const indexUrlPo = new PromiseOut<string>();
  let wid_po = new PromiseOut<string>();
  const getWinId = () => wid_po.promise;
  const setWinId = (win_id: string) => {
    if (wid_po.is_finished) {
      wid_po = PromiseOut.resolve(win_id);
    } else {
      wid_po.resolve(win_id);
    }
  };
  const delWinId = (win_id: string) => {
    if (wid_po.value === win_id) {
      wid_po = new PromiseOut();
    }
  };
  /**
   * 尝试打开gui，或者激活窗口
   */
  const tryOpenView = queue(async () => {
    /// 等待http服务启动完毕，获得入口url
    const url = await indexUrlPo.promise;
    const wid = await getWinId();
    if (all_webview_status.size === 0) {
      await sync_mwebview_status();
      console.log("mwebview_open=>", url, wid);
      await mwebview_open(wid, url);
    } else {
      console.log("mwebview_activate=>", url, wid);
      await mwebview_activate(wid);
    }
  });
  // 用来接收别人发过来的激活事件
  jsProcess.onActivity(async (ipcEvent) => {
    console.log(`${jsProcess.mmid} onActivity`, ipcEvent.data);
    const win_id = await apply_window();
    console.log("win_id=>", win_id);
    setWinId(win_id);
  });
  /// 如果主窗口已经激活，那么我就开始渲染
  jsProcess.onRenderer(async (ipcEvent) => {
    console.log(`${jsProcess.mmid} onRenderer`, ipcEvent.text);
    setWinId(ipcEvent.text);
    tryOpenView();
  });
  jsProcess?.onRendererDestroy?.(async (ipcEvent) => {
    console.log(`${jsProcess.mmid} onRendererDestroy`, ipcEvent.text);
    delWinId(ipcEvent.text);
  });

  jsProcess.onClose(() => {
    console.log("app后台被关闭。");
  });

  jsProcess.ready();

  //#region 启动http服务
  const plaocConfig = await PlaocConfig.init();

  const wwwServer = new Server_www(plaocConfig, await MiddlewareImporter.init(plaocConfig.config.middlewares?.www));
  const externalServer = new Server_external(await MiddlewareImporter.init(plaocConfig.config.middlewares?.external));
  const apiServer = new Server_api(getWinId, await MiddlewareImporter.init(plaocConfig.config.middlewares?.api));

  // quick action event
  jsProcess?.onShortcut?.(async (ipcEvent) => {
    console.log(`${jsProcess.mmid} onShortcut`, ipcEvent.text);
    const ipc = await externalServer.ipcPo.waitOpen();
    ipc.postMessage(ipcEvent);
  });

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
  await apiServer.getStartResult();
  const indexUrl = wwwStartResult.urlInfo.buildHtmlUrl(false, (url) => {
    url.pathname = "/index.html";
    url.searchParams.set(X_PLAOC_QUERY.EXTERNAL_URL, externalServer.token);
  });
  console.log("open in browser:", indexUrl.href);
  await Promise.all([wwwListenerTask, externalListenerTask, apiListenerTask]);
  indexUrlPo.resolve(indexUrl.href);
  open_main_window();
  //#endregion
};

try {
  await main();
} catch (e) {
  // todo 这里应该发送IpcError消息,这里是临时方案
  jsProcess.close(e)
  console.error("后端错误：", e);
}
