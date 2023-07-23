import { $DesktopAppMetaData, $WidgetMetaData } from "../types/app.type.ts";
import { searchWidget } from "./custom/search.widget.ts";
import { nativeFetch, nativeFetchStream } from "./fetch.ts";

export async function getAppInfo() {
  const res = await nativeFetch("/appsInfo");
  if (res.status !== 200) {
    console.error("请求失败：", res.status, res.statusText);
    return [];
  }
  return (await res.json()) as Promise<$DesktopAppMetaData[]>;
}

export function watchAppInfo() {
  return nativeFetchStream<$DesktopAppMetaData[]>("/observe/appsInfo");
}

export async function getWidgetInfo() {
  return [searchWidget] as $WidgetMetaData[];
}

/**点击打开JMM */
export async function openApp(id: string) {
  const res = await nativeFetch("/openAppOrActivate", {
    search: {
      app_id: id,
    },
  });
  return (await res.json()) === true;
}

/** 重击手势的反馈振动, 比如菜单键/惨案/3Dtouch */
export function vibrateHeavyClick() {
  return nativeFetch("/vibrateHeavyClick", {
    mmid: "haptics.sys.dweb",
  });
}

/**长按的退出按钮，这个会退出JMM后端 */
export async function quitApp(id: string) {
  const res = await await nativeFetch("/closeApp", {
    search: {
      app_id: id,
    },
  });
  return (await res.json()) === true;
}

/**卸载的是jmm所以从这里调用 */
export async function deleteApp(id: string) {
  await quitApp(id);
  return await nativeFetch("/uninstall", {
    search: {
      app_id: id,
    },
    mmid: "jmm.browser.dweb",
  });
}

export function shareApp(id: string) {
  nativeFetch("/shareApp", {
    search: {
      app_id: id,
    },
  });
}
