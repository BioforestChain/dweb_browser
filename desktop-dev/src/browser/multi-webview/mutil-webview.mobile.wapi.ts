import type { Remote } from "comlink";
import fs from "node:fs";
import path from "node:path";
import { pathToFileURL } from "node:url";
import type { Ipc } from "../../core/ipc/index.ts";
import type { MicroModule } from "../../core/micro-module.ts";
import { isElectronDev } from "../../helper/electronIsDev.ts";
import { locks } from "../../helper/locksManager.ts";
import {
  $NativeWindow,
  openNativeWindow,
} from "../../helper/openNativeWindow.ts";
import { $MMID } from "../../helper/types.ts";

type $APIS = typeof import("./assets/multi-webview.html.ts")["APIS"];

export type $WApi = { nww: $NativeWindow; apis: Remote<$APIS> };
/**
 * WARN 这里的 wapis 是全局共享的，也就是说无法实现逻辑上的多实例，务必注意
 *
 * 要实现也可以，这里的key需要额外有一个根据 mwebview 的归组逻辑，比如： file://mwebview.browser.dweb/uid 获得一个实例编号
 */
export const _mmid_wapis_map = new Map<$MMID, $WApi>();
export const getAllWapis = () => _mmid_wapis_map.entries();
export const deleteWapis = (filter: (wapi: $WApi, mmid: $MMID) => boolean) => {
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

export const nwwGetFromMmid = (mmid: $MMID) => {
  return _mmid_wapis_map.get(mmid)?.nww;
};

const init_preload_js = (async () => {
  console.log(Electron.app.getPath("appData"));
  const preload_js_path = path.join(
    Electron.app.getPath("appData"),
    `${Electron.app.getName()}/internal/${Electron.app.getVersion()}/mutil-webview.preload.js`,
    "mutil-webview.preload.js"
  );
  console.log(preload_js_path);
  /// preload.js 在启动后，进行第一次写入；如果在开发模式下，那么每次启动都强制写入
  if (isElectronDev || fs.existsSync(preload_js_path) === false) {
    /// 引入代码，编译后的，然后保存到外部文件中
    const preloadCode = (
      await import("./assets/mutil-webview.preload.ts")
    ).default.toString();
    fs.mkdirSync(path.dirname(preload_js_path), { recursive: true });
    fs.writeFileSync(preload_js_path, `(${preloadCode})()`);
  }
  return pathToFileURL(preload_js_path).href;
})();

export function forceGetWapis(this: MicroModule, ipc: Ipc, root_url: string) {
  ipc.onClose(() => {
    // 是否会出现 一个 JsMicroModule 打开其他的 JsMicroModule
    // 的情况，如果是这样的话会出现一个 borserWindow 内会包含连个应用
    // 当前判断不可以 是不可以 一个 browserWindow 内只会以一个 应用
    const wapi = _mmid_wapis_map.get(ipc.remote.mmid);
    wapi?.nww.close();
    _mmid_wapis_map.delete(ipc.remote.mmid);
  });

  return locks.request(
    "multi-webview-get-window-" + ipc.remote.mmid,
    async () => {
      let wapi = _mmid_wapis_map.get(ipc.remote.mmid);
      if (wapi === undefined) {
        this.nativeFetch(`file://js.browser.dweb/bw?action=show`);
        const diaplay = Electron.screen.getPrimaryDisplay();
        const nww = await openNativeWindow(root_url + `&uid=${ipc.uid}`, {
          webPreferences: {
            webviewTag: true,
          },
          // autoHideMenuBar: true,
          // 测试代码
          width: 375,
          height: 800,
          x: 0,
          y: (diaplay.size.height - 800) / 2,
          // frame: false,
        });

        nww.on("close", () => {
          _mmid_wapis_map.delete(ipc.remote.mmid);
          if (_mmid_wapis_map.size <= 0) {
            this.nativeFetch(`file://js.browser.dweb/bw?action=hide`);
          }
        });

        const apis = nww.getApis<$APIS>();

        apis.preloadAbsolutePathSet(await init_preload_js);

        _mmid_wapis_map.set(ipc.remote.mmid, (wapi = { nww, apis }));
      }
      return wapi;
    }
  );
}
