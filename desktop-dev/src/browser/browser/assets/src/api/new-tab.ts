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


