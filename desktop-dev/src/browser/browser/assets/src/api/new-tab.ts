import { nativeFetch } from "./fetch";

export async function getAppInfo() {
  const res = await nativeFetch("/appsInfo");
  if (res.status !== 200) {
    console.error("请求失败：", res.statusText);
    return;
  }
  return await res.json();
}

export function clickApp(id:string) {
  nativeFetch("/openApp",{
    search:{
      app_id:id
    }
  })
}

/** 重击手势的反馈振动, 比如菜单键/惨案/3Dtouch */
export function vibrateHeavyClick() {
  return nativeFetch("/vibrateHeavyClick",{
    mmid:"haptics.sys.dweb"
  })
}

/**长按的退出按钮，这个会退出后端 */
export function quitApp(id:string) {
  nativeFetch("/deleteApp",{
    search:{
      app_id:id
    }
  })
}

export function deleteApp(id:string) {
  nativeFetch("/deleteApp",{
    search:{
      app_id:id
    }
  })
}

export function shareApp(id:string) {
  nativeFetch("/shareApp",{
    search:{
      app_id:id
    }
  })
}
