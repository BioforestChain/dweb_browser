import { nativeFetch } from "./fetch.ts";

export async function getAppInfo() {
  const res = await nativeFetch("/appsInfo");
  if (res.status !== 200) {
    console.error("请求失败：", res.statusText);
    return;
  }
  return await res.json();
}

/**点击打开JMM */
export function clickApp(id: string) {
  nativeFetch("/openAppOrActivate", {
    search: {
      app_id: id,
    },
  });
}

/** 重击手势的反馈振动, 比如菜单键/惨案/3Dtouch */
export function vibrateHeavyClick() {
  nativeFetch("/vibrateHeavyClick", {
    mmid: "haptics.sys.dweb",
  });
}

/**长按的退出按钮，这个会退出JMM后端 */
export async function quitApp(id: string) {
  await nativeFetch("/closeApp", {
    search: {
      app_id: id,
    },
    mmid: "jmm.browser.dweb",
  });
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

export async function detailApp(id: string) {
  return await nativeFetch("/detailApp", {
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
