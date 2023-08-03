import { $WidgetAppData, $WidgetCustomData } from "../types/app.type.ts";
import { searchWidget } from "./custom/search.widget.ts";
import { nativeFetch, nativeFetchStream } from "./fetch.ts";

export async function readAccept(ext: string = "") {
  const { accept } = await nativeFetch<{ accept: string }>(`/readAccept.${ext}`, {});
  return accept
    .split(";")[0]!
    .split(",")
    .map((mime) => {
      const mimeLower = mime.toLowerCase();
      if (mime.includes("*")) {
        const mimeReg = new RegExp(mime.replace(/\*/g, ".+"), "i");
        return (type: string) => mimeLower === type.toLowerCase() || mimeReg.test(type);
      }
      return (type: string) => mimeLower === type.toLowerCase();
    });
}

let _readAcceptSvg: undefined | ReturnType<typeof readAccept>;
export const readAcceptSvg = () => (_readAcceptSvg ??= readAccept("svg"));

export function watchDesktopAppInfo() {
  return nativeFetchStream<$WidgetAppData[]>("/desktop/observe/apps");
}
export function watchTaskbarAppInfo() {
  return nativeFetchStream<$WidgetAppData[]>("/taskbar/observe/apps");
}

export async function getWidgetInfo() {
  return [searchWidget] as $WidgetCustomData[];
}

/**点击打开JMM */
export async function openApp(id: string) {
  return await nativeFetch<boolean>("/openAppOrActivate", {
    search: {
      app_id: id,
    },
  });
}

export async function toggleMaximize(id: string) {
  return await nativeFetch<boolean>("/toggleMaximize", {
    search: {
      app_id: id,
    },
  });
}

/** 重击手势的反馈振动, 比如菜单键/惨案/3Dtouch */
export async function vibrateHeavyClick() {
  return nativeFetch("/vibrateHeavyClick", {
    mmid: "haptics.sys.dweb",
  });
}

/**长按的退出按钮，这个会退出JMM后端 */
export async function quitApp(id: string) {
  return await await nativeFetch<boolean>("/closeApp", {
    search: {
      app_id: id,
    },
  });
}

/**卸载的是jmm所以从这里调用 */
export async function deleteApp(id: string) {
  await quitApp(id);
  return nativeFetch<boolean>("/uninstall", {
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

export function resizeTaskbar(width: number, height: number) {
  return nativeFetch<{ width: number; height: number }>("/taskbar/resize", { search: { width, height } });
}

export function toggleDesktopView() {
  return nativeFetch<Array<{ height: number; width: number; x: number; y: number }>>("/taskbar/toggle-desktop-view");
}
