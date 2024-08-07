import { $TaskBarState, $WidgetAppData, $WidgetCustomData } from "../types/app.type.ts";
import { searchWidget } from "./custom/search.widget.ts";
import { buildApiRequestArgs, nativeFetch, nativeFetchStream } from "./fetch.ts";

export function readPathFile(path: string) {
  return path.startsWith("file:")
    ? buildApiRequestArgs("/readFile", {
        search: {
          url: path,
        },
      })[0].href
    : path;
}

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
export function watchTaskBarStatus() {
  return nativeFetchStream<$TaskBarState>("/taskbar/observe/status");
}

/**
 * 获取search组件
 * @returns
 */
export function getWidgetInfo() {
  return [searchWidget] as $WidgetCustomData[];
}

/**点击打开JMM */
export function openApp(id: string) {
  return fetch(
    buildApiRequestArgs("/openAppOrActivate", {
      search: {
        app_id: id,
      },
    })[0].href,
  );
}

export function doToggleTaskbar(toggle?: boolean) {
  return nativeFetch<boolean>("/taskbar/toggle-float-button-mode", {
    search: {
      open: toggle,
    },
  });
}

/** 点击打开应用详情下载页 */
export async function detailApp(id: string) {
  return await nativeFetch<boolean>("/detail", {
    search: {
      app_id: id,
    },
    mmid: "jmm.browser.dweb",
  });
}

export function openBrowser(url: string) {
  return nativeFetch<boolean>("/openBrowser", {
    search: {
      url: url,
    },
    mmid: "desk.browser.dweb",
  });
}

export function showToast(message: string) {
  nativeFetch("/showToast", {
    search: {
      message: message,
    },
  });
}

export function toggleMaximize(id: string) {
  return nativeFetch<boolean>("/toggleMaximize", {
    search: {
      app_id: id,
    },
  });
}

/** 重击手势的反馈振动, 比如菜单键/惨案/3Dtouch */
export function vibrateHeavyClick() {
  return nativeFetch("/vibrateHeavyClick", {
    mmid: "haptics.sys.dweb",
  });
}

/**长按的退出按钮，这个会退出JMM后端 */
export function quitApp(id: string) {
  return nativeFetch<boolean>("/closeApp", {
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

export function deleteWebLink(mmid: string) {
  return nativeFetch<Response>("/removeWebLink", {
    search: {
      app_id: mmid,
    },
    mmid: "desk.browser.dweb",
  });
}

export function shareApp(id: string) {
  nativeFetch("/shareApp", {
    search: {
      app_id: id,
    },
  });
}

let preWidth = NaN;
let preHeight = NaN;
export function resizeTaskbar(width: number, height: number, force = false) {
  if (width === preWidth && height === preHeight && !force) {
    return { width: preWidth, height: preHeight };
  }
  preWidth = width;
  preHeight = height;
  return nativeFetch<{ width: number; height: number }>("/taskbar/resize", { search: { width, height } });
}

export function toggleDesktopView() {
  return nativeFetch<Array<{ height: number; width: number; x: number; y: number }>>("/taskbar/toggle-desktop-view");
}

export function toggleDragging(dragging: boolean) {
  return nativeFetch<{ width: number; height: number }>("/taskbar/dragging", { search: { dragging } });
}
