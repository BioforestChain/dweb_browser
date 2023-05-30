import type { Remote } from "comlink";
import path from "node:path";
import { pathToFileURL } from "node:url";
import type { Ipc } from "../../core/ipc/index.ts";
import { locks } from "../../helper/locksManager.ts";
import {
  $NativeWindow,
  openNativeWindow,
} from "../../helper/openNativeWindow.ts";

type $APIS = typeof import("./assets/multi-webview.html.ts")["APIS"];

export type $WApi = { nww: $NativeWindow; apis: Remote<$APIS> };
/**
 * WARN 这里的 wapis 是全局共享的，也就是说无法实现逻辑上的多实例，务必注意
 *
 * 要实现也可以，这里的key需要额外有一个根据 mwebview 的归组逻辑，比如： file://mwebview.sys.dweb/uid 获得一个实例编号
 */
const _mmid_wapis_map = new Map<$MMID, $WApi>();
export const getAllWapis = () => _mmid_wapis_map.entries();
export const deleteWapis = (
  filter: (wapi: $WApi, mmid: $MMID) => boolean
) => {
  for (const [mmid, wapi] of _mmid_wapis_map) {
    if (filter(wapi, mmid)) {
      wapi.nww.close();
      _mmid_wapis_map.delete(mmid);
    }
  }
};
export const apisGetFromMmid = (mmid: $MMID) => {
  return _mmid_wapis_map.get(mmid)?.apis;
};

export const forceGetWapis = (ipc: Ipc, root_url: string) => {
  ipc.onClose(() => {
    // 是否会出现 一个 JsMicroModule 打开其他的 JsMicroModule 
    // 的情况，如果是这样的话会出现一个 borserWindow 内会包含连个应用
    // 当前判断不可以 是不可以 一个 browserWindow 内只会以一个 应用
    const wapi = _mmid_wapis_map.get(ipc.remote.mmid);
    const devToolsWin = wapi?.nww._devToolsWin.values()
    if(devToolsWin !== undefined){
      Array.from(devToolsWin).forEach(item => {
        item.close();
      })
    }
    wapi?.nww.close();
    _mmid_wapis_map.delete(ipc.remote.mmid)
  })

  return locks.request("multi-webview-get-window-" + ipc.remote.mmid, async () => {
    let wapi = _mmid_wapis_map.get(ipc.remote.mmid);
    if (wapi === undefined) {
      const diaplay = Electron.screen.getPrimaryDisplay();

      const nww = await openNativeWindow(root_url, {
        webPreferences: {
          webviewTag: true,
        },
        autoHideMenuBar: true,
        // 测试代码
        width: 375,
        height: 800,
        x: 0,
        y: (diaplay.size.height - 800) / 2,
        frame: false,
      });

      const apis = nww.getApis<$APIS>();
      const absolutePath = pathToFileURL(
        path.resolve(__dirname, "./assets/preload.js")
      ).href;
      /// TIP: 这里通过类型强行引用 preload，目的是确保依赖关系，使得最终能产生编译内容
      type _Preload = typeof import("./assets/preload.ts");
      apis.preloadAbsolutePathSet(absolutePath);

      _mmid_wapis_map.set(ipc.remote.mmid, (wapi = { nww, apis }));
    }
    return wapi;
  });
};
