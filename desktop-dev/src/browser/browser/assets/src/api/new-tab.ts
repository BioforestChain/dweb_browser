import { nativeFetch } from "./fetch";

export async function getAppInfo() {
  const res = await nativeFetch("/appsInfo");
  if (res.status !== 200) {
    console.error("请求失败：", res.statusText);
    return;
  }
  return await res.json();
}

export function clickApp(id: string) {
  nativeFetch("/openApp", {
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

/**长按的退出按钮，这个会退出后端 */
export async function quitApp(id: string) {
  await nativeFetch("/closeApp", {
    search: {
      app_id: id,
    },
  });
}

export async function deleteApp(id: string) {
  await quitApp(id)
  return await nativeFetch("/uninstall", {
    search: {
      app_id: id,
      mmid: "jmm.browser.dweb",
    },
  });
}

export async function detailApp(id: string) {
  console.log("/detailAPp",id)
  return await nativeFetch("/detailApp", {
    search: {
      app_id: id,
    },
  });
}

export function shareApp(id: string) {
  nativeFetch("/shareApp", {
    search: {
      app_id: id,
    },
  });
}
